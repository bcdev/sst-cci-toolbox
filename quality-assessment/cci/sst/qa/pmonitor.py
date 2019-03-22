# -*- coding: utf-8 -*-

"""Workflow engine and dispatcher for processing requests"""
from cci.sst.qa.threadpool import ThreadPool, WorkRequest, NoResultsPending

__author__ = "Martin Böttcher, Brockmann Consult GmbH"
__copyright__ = "Copyright 2016, Brockmann Consult GmbH"
__license__ = "For use with Calvalus processing systems"
__version__ = "1.4"
__email__ = "info@brockmann-consult.de"
__status__ = "Production"

# changes in 1.1
# observe_step method added to allow rmonitor to override and collect passed events
# changes in 1.2
# wait_for_idle added to support multi-step ingestion
# changes in 1.3
# python3 compatibility using six
# changes in 1.4
# extended for asynchronous batch system interface, external IDs, and status polling

import glob
import os
import subprocess
import threading
import traceback

import sys
import time
from six.moves import reduce



class PMonitor:
    """
    Handles tasks and their dependencies (as formal inputs and outputs) and executes them on a thread pool.
    Maintains
      a thread pool with a task queue of mature tasks, those with inputs available
      a backlog of tasks not yet mature
      a report file that records all completed calls and the paths of output product (set) names
      a set of commands executed in previous runs listed in the initial report, not updated
      a map of bound product (set) names, mapped to None (same path) or a set of paths, same as in report file
      a map of product set names to the number of yet missing outputs of non-collating tasks
      a list of host resources with capacity and current load, ordered by capacity-load
      a list of type resources with capacity and current load
      a mutex to protect access to backlog, report, paths, counts
    Usage:
        ...
        pm = PMonitor(allInputs, request=year+'-'+month, hosts=[('localhost',2),('phost2',4)])
        for day in days:
            ...
            pm.execute('bin/meris-l2.sh', l2Inputs, [l2Output], priority=1, collating=False)
            ...
            pm.execute('bin/meris-l3.sh', [l2Output], [dailyOutput], priority=2)
        ...
        pm.execute('bin/meris-aggregation.sh', dailyOutputs, [monthlyOutput], priority=3)
        pm.wait_for_completion()
    """

    class Constraint:
        """
        Triple of host name, capacity, and load, or of request type, capacity, and load
        """
        name = None
        capacity = None
        load = None

        def __init__(self, name, capacity):
            self.name = name
            self.capacity = capacity
            self.load = 0

        def __cmp__(self, other):
            if self.load > other.load:
                return 1
            elif self.load < other.load:
                return -1
            else:
                return 0

        def __eq__(self, other):
            return self.load == other.load

        def __ne__(self, other):
            return self.load != other.load

        def __lt__(self, other):
            return self.load < other.load

        def __le__(self, other):
            return self.load <= other.load

        def __gt__(self, other):
            return self.load > other.load

        def __ge__(self, other):
            return self.load >= other.load

    class State:
        UNKNOWN = 'UNKNOWN'
        WAITING = 'WAITING'
        RUNNING = 'RUNNING'
        DONE = 'DONE'
        SUCCEEDED = 'SUCCEEDED'
        FAILED = 'FAILED'
        CANCELLED = 'CANCELLED'
        DROPPED = 'DROPPED'
        NOT_FOUND = 'NOT_FOUND'
        _LOST = '_LOST'

    class Args:
        def __init__(self, call, task_id, parameters, inputs, outputs, host, log_prefix, async, external_id=None, priority=1):
            self.call = call
            self.task_id = task_id
            self.parameters = parameters
            self.inputs = inputs
            self.outputs = outputs
            self.host = host
            self.log_prefix = log_prefix
            self.async = async
            self.external_id = external_id
            self.priority = priority

        # threadpool requires list of arguments
        @staticmethod
        def get_call(args): return args[0]

        @staticmethod
        def set_call(args, value): args[0] = value

        @staticmethod
        def get_task_id(args): return args[1]

        @staticmethod
        def set_task_id(args, value): args[1] = value

        @staticmethod
        def get_parameters(args): return args[2]

        @staticmethod
        def set_parameters(args, value): args[2] = value

        @staticmethod
        def get_inputs(args): return args[3]

        @staticmethod
        def set_inputs(args, value): args[3] = value

        @staticmethod
        def get_outputs(args): return args[4]

        @staticmethod
        def set_outputs(args, value): args[4] = value

        @staticmethod
        def get_host(args): return args[5]

        @staticmethod
        def set_host(args, value): args[5] = value

        @staticmethod
        def get_log_prefix(args): return args[6]

        @staticmethod
        def set_log_prefix(args, value): args[6] = value

        @staticmethod
        def get_async(args): return args[7]

        @staticmethod
        def set_async(args, value): args[7] = value

        @staticmethod
        def get_external_id(args): return args[8]

        @staticmethod
        def set_external_id(args, value): args[8] = value

        @staticmethod
        def get_priority(args): return args[9]

        @staticmethod
        def set_priority(args, value): args[9] = value

    def __init__(self, inputs, request='', hosts=[('localhost', 4)], types=[], weights=[], swd=None, cache=None, logdir='.', simulation=False, delay=None, fair=True, script=None, polling=None,
                 period=5, async=None):
        """
        Initiates monitor, marks inputs, reads report, creates thread pool
        inputs: the apriori conditions
        request: prefix name of the files for status report, pid and log
        hosts: list of constraints of hosts and concurrent steps
        types: list of constraints of steps and their concurrency
        weights: list of constraints of steps and their respective cores requirement, defaults to empty list
        swd: software directory, defaults to None for steps to be found via PATH
        cache: directory where working directories are created for steps, defaults to None for no working dir creation
        logdir: directory for step log files
        simulation: switch to skip actual execution of step, defaults to False
        delay: milliseconds to wait after step scheduling to avoid races between concurrent steps, defaults to None for no waiting
        fair: handles steps in sequence of their generation (and avoids looking through complete list of steps), defaults to True
        script: generic step execution script to be used as prefix of each command line, defaults to None for the different steps being executable scripts themselves
        polling: generic status polling script called with a list of external request identifiers to inquire the status in form of CSV <id>,<state>[,<msg>]\n
        period: number of seconds between polling calls for asynchronous steps
        async: default value for steps, whether start shall be synchronous or asynchronous, defaults to True if polling is set, False else
        """
        self._mutex = threading.RLock()
        # print '... mutex 1 acquiring'
        with self._mutex:
            # print '... mutex 1 acquired'
            self._backlog = []
            self._running = {}
            self._failed = []
            self._created = 0
            self._processed = 0
            self._report = None
            self._commands = set([])
            self._paths = dict([])
            self._counts = dict([])

            self._hostConstraints = PMonitor._constraints_of(hosts)
            self._typeConstraints = PMonitor._constraints_of(types)
            self._weightConstraints = PMonitor._constraints_of(weights)
            self._request = request
            self._swd = swd
            self._cache = cache
            self._logdir = logdir
            self._simulation = simulation
            self._delay = delay
            self._fair = fair
            self._script = script
            self._polling = polling
            self._period = period
            self._async = async
            self._mark_inputs(inputs)
            concurrency = sum([host[1] for host in hosts])
            self._pool = ThreadPool(concurrency, poll_timeout=period)
            self._backlog_condition = None

            os.system('mkdir -p ' + logdir)
            self._status = open(request + '.status', 'w')
            self._maybe_read_report(request + '.report')
            if polling:
                self._maybe_read_status(request + '.status')
        # print '... mutex 1 released'

    def execute(self, call, inputs, outputs, parameters=[], priority=1, collating=True, async=None, logprefix=None):
        """
        Schedules task `call parameters inputs outputs`, either a single collating call or one call per input
        """
        if async == None:
            async = self._async if self._async != None else self._polling != None
        # print '... mutex 2 acquiring'
        with self._mutex:
            # print '... mutex 2 acquired'
            if logprefix is None:
                logprefix = call[call.rfind('/') + 1:]
                if logprefix.endswith('.sh') or logprefix.endswith('.py'):
                    logprefix = logprefix[:-3]
            self._created += 1
            args = [call, self._created, parameters, None, outputs, None, logprefix, async]
            request = WorkRequest(None, args, priority=priority, requestID=self._created)
            for o in outputs:
                if o in self._counts:
                    self._counts[o] += 1
                else:
                    self._counts[o] = 1

            if self._all_inputs_available(inputs) and self._constraints_fulfilled(request):
                input_paths = self._paths_of(inputs)
                if collating:
                    request.callable = self._process_step
                    PMonitor.Args.set_inputs(request.args, input_paths)
                    self._pool.putRequest(request)
                    if self._delay is not None:
                        time.sleep(self._delay)
                else:
                    for i in range(len(input_paths)):
                        if i == 0:
                            request.callable = self._process_step
                            PMonitor.Args.set_inputs(request.args, input_paths[0:1])
                        else:
                            self._created += 1
                            args = [call, self._created, parameters, input_paths[i:i + 1], outputs, None, logprefix, async]
                            request = WorkRequest(self._process_step, args,
                                                             priority=priority, requestID=self._created)
                            for o in outputs:
                                self._counts[o] += 1
                        if i == 0 or self._constraints_fulfilled(request):
                            self._pool.putRequest(request)
                            if self._delay is not None:
                                time.sleep(self._delay)
                        else:
                            self._backlog.append(request)
            else:
                if collating:
                    request.callable = self._translate_step
                else:
                    request.callable = self._expand_step
                PMonitor.Args.set_inputs(request.args, inputs)
                self._backlog.append(request)
        # print '... mutex 2 released'

    def wait_for_completion(self, stop_if_idle=True):
        """
        Waits until all scheduled tasks are run, then returns
        """
        # print '... mutex 3 acquiring'
        with self._mutex:
            # print '... mutex 3 acquired'
            self._write_status(with_backlog=False)
            # print '... mutex 3 released'
        if not self._polling:
            while True:
                self._pool.wait()
                # print '... mutex 3 acquiring'
                with self._mutex:
                    # print '... mutex 3 acquired'
                    if not self._pool.workRequests:
                        break
                # print '... mutex 3 released'
        else:
            time.sleep(self._period / 10.0)
            while True:
                if self._polling:
                    self._inquire_status()
                try:
                    self._pool.poll(True)  # hopefully times out
                except NoResultsPending:
                    pass
                time.sleep(self._period)
                if stop_if_idle:
                    # print '... mutex 3 acquiring'
                    with self._mutex:
                        # print '... mutex 3 acquired'
                        if not self._pool.workRequests and not self._running:
                            break
                    # print '... mutex 3 released'
        # print '... mutex 3 acquiring'
        with self._mutex:
            # print '... mutex 3 acquired'
            self._write_status(with_backlog=True)
            # print '... mutex 3 released'
        # self._status.close()
        return int(bool(self._failed or self._backlog))

    def wait_for_idle(self, calls):
        """
        Waits until no requests of the listed call types are in backlog, then returns
        """
        if not self._backlog_condition:
            self._backlog_condition = threading.Condition()
        while True:
            # print '... mutex 3 acquiring'
            with self._mutex:
                # print '... mutex 3 acquired'
                in_backlog = None
                for type in self._typeConstraints:
                    # print 'type constraint ' + type.name + ' ' + str(type.load) + '/' + str(type.capacity)
                    if type.name in calls:
                        if type.load >= type.capacity:
                            in_backlog = type.name
                            break
                if not in_backlog:
                    # print "no more backlog, return from waiting"
                    break;
                # print "waiting for " + in_backlog
            # print '... mutex 3 released'
            with self._backlog_condition:
                # print '... backlog condition wait'
                self._backlog_condition.wait()
                # print '... backlog condition resume'

    @staticmethod
    def _constraints_of(config):
        """
        Converts configuration into list of constraints
        """
        constraints = [PMonitor.Constraint(name_limit[0], name_limit[1]) for name_limit in config]
        constraints.sort()
        return constraints

    def _maybe_read_report(self, report_path):
        """
        Reads report containing lines with command line calls and lines with an output path of a product name, e.g.
          bin/meris-l3.sh /home/boe/eodata/MER_WV__2P/v01/2010/01/25 /home/boe/eodata/MER_WV__3P/v01/2010/01/25/meris-l3-daily-25.dat
          #output /home/boe/eodata/MER_WV__3P/v01/2010/01/25/meris-l3-daily-25.dat /home/boe/eodata/MER_WV__3P/v01/2010/01/25/meris-l3-daily-25.dat.real
        """
        if glob.glob(report_path):
            self._report = open(report_path, 'r+')
            for line in self._report.readlines():
                if line.startswith('#output '):
                    w = line.split()
                    name = w[1]
                    paths = w[2:]
                    self._bind_output(name, paths)
                else:
                    self._commands.add(line[:-1])
        else:
            self._report = open(report_path, 'w')

    def _maybe_read_status(self, status_path):
        """Reads status file containing lines with exernal ID and command line call"""
        with open(status_path, 'r') as file:
            for line in file.readlines():
                if line.startsWith('r ['):
                    external_id = line[len('r ['):line.find('] ')]
                    command = line[line.find('] ') + len('] '):]
                    self._running[command] = external_id

    def _write_status(self, with_backlog=False):
        self._status.seek(0)
        # pending = len(self._pool.workRequests) - len(self._running)
        self._status.write('{0} created, {1} running, {2} backlog, {3} processed, {4} failed\n'. \
                           format(self._created, len(self._running), len(self._backlog), self._processed, len(self._failed)))
        for l in self._failed:
            self._status.write('f {0}\n'.format(l))
        for l in self._running:
            if isinstance(self._running[l], PMonitor.Args):
                self._status.write('r [{0}] {1}\n'.format(self._running[l].external_id, l))
            elif isinstance(self._running[l], str):
                self._status.write('r [{0}] {1}\n'.format(self._running[l], l))
            else:
                self._status.write('r {0}\n'.format(l))
        if with_backlog:
            for r in self._backlog:
                self._status.write('b {0} {1} {2} {3}\n'.format(PMonitor.Args.get_call(r.args),
                                                                ' '.join(PMonitor.Args.get_parameters(r.args)),
                                                                ' '.join(PMonitor.Args.get_inputs(r.args)),
                                                                ' '.join(PMonitor.Args.get_outputs(r.args))))
        self._status.truncate()
        self._status.flush()

    def _expand_step(self, call, task_id, parameters, inputs, outputs, host, log_prefix):
        """
        Marker for non-collating tasks, formal callable
        """
        raise NotImplementedError('must never be called')

    def _translate_step(self, call, task_id, parameters, inputs, outputs, host, log_prefix):
        """
        Marker for tasks that require input path translation, formal callable
        """
        raise NotImplementedError('must never be called')

    def _process_step(self, call, task_id, parameters, inputs, outputs, host, log_prefix, async):
        """
        Callable for tasks, marker for simple or collating tasks.
        looks up call in commands from report, maybe skips execution
        executes call by os call, scans process stdout for `output=...`
        updates report, maintains products, and schedules mature tasks
        """
        try:
            if self._script:
                command = '{0} {1} {2} {3} {4}'.format(self._path_of_call(self._script), call, ' '.join(parameters), ' '.join(inputs), ' '.join(outputs))
            else:
                command = '{0} {1} {2} {3}'.format(self._path_of_call(call), ' '.join(parameters), ' '.join(inputs), ' '.join(outputs))
            if command in self._commands:
                self._skip_step(call, command, outputs, host)
                self._observe_step(call, inputs, outputs, parameters, 0)
            elif command in self._running and isinstance(self._running[command], str) and not self._simulation:
                self._running[command] = PMonitor.Args(call, task_id, parameters, inputs, outputs, host, log_prefix, async, external_id=self._running[command])
                sys.__stdout__.write('reconsidering {0}\n'.format(command))
            else:
                self._prepare_step(command)
                output_paths = []
                if not self._simulation:
                    code = self._run_step(task_id, host, command, output_paths, log_prefix, async)
                else:
                    code = 0
                if async and code == 0 and not self._simulation:
                    self._running[command] = PMonitor.Args(call, task_id, parameters, inputs, outputs, host, log_prefix, async, external_id=output_paths[0])
                elif async:
                    self._running[command] = PMonitor.Args(call, task_id, parameters, inputs, outputs, host, log_prefix, async, external_id='pending')
                else:
                    self._finalise_step(call, code, command, host, output_paths, outputs)
                    self._observe_step(call, inputs, outputs, parameters, code)
                # print '... mutex 6 acquiring'
                with self._mutex:
                    # print '... mutex 6 acquired'
                    self._write_status()
                # print '... mutex 6 released'
        except Exception as e:
            print('internal error: ' + str(e))
            traceback.print_exc(file=sys.stdout)

    def _skip_step(self, call, command, outputs, host):
        """
        Marks outputs of step and schedules mature tasks
        """
        # print '... mutex 4 acquiring'
        with self._mutex:
            # print '... mutex 4 acquired'
            sys.__stdout__.write('skipping {0}\n'.format(command))
            self._release_constraint(call, host)
            self._mark_outputs(outputs)
            self._check_for_mature_tasks()
            self._processed += 1
        # print '... mutex 4 released'

    def _prepare_step(self, command):
        """
        Updates status, selects host and returns it
        """
        # print '... mutex 5 acquiring'
        with self._mutex:
            # print '... mutex 5 acquired'
            self._running[command] = None
            self._write_status()
        # print '... mutex 5 released'

    def _run_step(self, task_id, host, command, output_paths, log_prefix, async):
        """
        Executes command on host, collects output paths if any, returns exit code
        """
        wd = self._prepare_working_dir(task_id)
        process = PMonitor._start_processor(command, host, wd)
        self._trace_processor_output(output_paths, process, task_id, wd, log_prefix, async)
        process.stdout.close()
        code = process.wait()
        if code == 0 and not async and not self._cache is None and 'cache' in wd:
            subprocess.call(['rm', '-rf', wd])
        return code

    def _finalise_step(self, call, code, command, host, output_paths, outputs):
        """
        releases host and type resources, updates report, schedules mature steps, handles failure
        """
        # print '... mutex 6 acquiring'
        with self._mutex:
            # print '... mutex 6 acquired'
            self._release_constraint(call, host)
            self._running.pop(command)
            if code == 0:
                self._report.write(command + '\n')
                self._report_and_bind_outputs(outputs, output_paths)
                self._report.flush()
                self._processed += 1
            else:
                self._failed.append(command)
                sys.__stderr__.write('failed {0}\n'.format(command))
            self._check_for_mature_tasks()
        # print '... mutex 6 released'

    def _observe_step(self, call, inputs, outputs, parameters, code):
        pass

    def _inquire_status(self):
        with self._mutex:
            running = {x: self._running[x] for x in self._running if isinstance(self._running[x], PMonitor.Args)}
        if not running:
            return
        external_ids = [x.external_id for x in running.values() if x.external_id != 'pending']
        inquiry = {}
        # retrieve status with polling script
        if external_ids:
            print('retrieving status with ' + self._polling + ' ' + ' '.join(external_ids))
            process = subprocess.Popen(self._polling + ' ' + ' '.join(external_ids),
                                       shell=True, bufsize=1, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            for l in process.stdout:
                line = l.decode()
                print(line[:-1])
                p1 = line.find(',')
                p2 = line.find(',', p1 + 1)
                if p1 >= 0:
                    external_id = line[:p1].strip()
                    if p2 >= 0:
                        status = line[p1 + 1:p2].strip()
                    else:
                        status = line[p1 + 1:].strip()
                    inquiry[external_id] = status
            process.stdout.close()
            code = process.wait()
            if code != 0:
                print(self._polling + ' ' + ' '.join(external_ids) + ' failed')
                return
        # determine status of running requests by inspecting retrieval result
        some_final_steps = False
        for command in running:
            args = running[command]
            status = inquiry[args.external_id] if args.external_id in inquiry else PMonitor.State._LOST
            if status == PMonitor.State.DONE or status == PMonitor.State.SUCCEEDED:
                if self._cache:
                    wd = self._cache + '/' + self._request + '/' + '{0:04d}'.format(args.task_id)
                    if 'cache' in wd:
                        subprocess.call(['rm', '-rf', wd])
                self._finalise_step(args.call, 0, command, args.host, [], args.outputs)
                self._observe_step(args.call, args.inputs, args.outputs, args.parameters, 0)
                some_final_steps = True
            elif status == PMonitor.State.FAILED or status == PMonitor.State.CANCELLED:
                self._finalise_step(args.call, 1, command, args.host, [], args.outputs)
                self._observe_step(args.call, args.inputs, args.outputs, args.parameters, 1)
                some_final_steps = True
            elif status == PMonitor.State.DROPPED or status == PMonitor.State.NOT_FOUND or status == PMonitor.State._LOST:
                with self._mutex:
                    # release and re-schedule request
                    self._release_constraint(args.call, args.host)
                    self._running.pop(command)
                    args2 = [args.call, args.task_id, args.parameters, args.inputs, args.outputs, None, args.log_prefix, True]
                    request = WorkRequest(self._process_step, args2,
                                                     priority=1, requestID=args.task_id)
                    self._backlog.append(request)
                    self._check_for_mature_tasks()
                some_final_steps = True
        if some_final_steps:
            with self._mutex:
                self._write_status()

    def _prepare_working_dir(self, task_id):
        """Creates working directory in .../cache/request-id/task-id"""
        if self._cache is None:
            wd = '.'
        else:
            wd = self._cache + '/' + self._request + '/' + '{0:04d}'.format(task_id)
        if not os.path.exists(wd):
            os.makedirs(wd)
        return wd

    @staticmethod
    def _start_processor(command, host, wd):
        """starts subprocess either locally or via ssh, returns process handle"""
        if host == 'localhost':
            cmd = command
        else:
            cmd = 'ssh ' + host + " 'mkdir -p " + wd + ';' + " cd " + wd + ';' + command + "'"
        sys.__stdout__.write('executing {0}'.format(cmd + '\n'))
        process = subprocess.Popen(cmd, shell=True, bufsize=1, cwd=wd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        return process

    def _trace_processor_output(self, output_paths, process, task_id, wd, log_prefix, async):
        """
        traces processor output, recognises 'output=' lines, writes all lines to trace file in working dir.
        for async calls reads external ID from stdout.
        """
        if self._cache is None or self._logdir != '.':
            trace = open('{0}/{1}-{2:04d}.out'.format(self._logdir, log_prefix, task_id), 'w')
        else:
            trace = open('{0}/{1}-{2:04d}.out'.format(wd, log_prefix, task_id), 'w')
        line = None
        for l in process.stdout:
            line = l.decode()
            if line.startswith('output='):
                output_paths.append(line[7:].strip())
            trace.write(line)
            trace.flush()
        trace.close()
        if async and line:
            # assumption that last line contains external ID, with stderr mixed with stdout
            output_paths[:] = []
            output_paths.append(line.strip())

    def _check_for_mature_tasks(self):
        """
        Checks tasks in backlog whether inputs are (now) all bound in products map
        adds these mature tasks to thread pool, removes them from backlog
        distinguishes collating and non-collating tasks by the callable used
        generates one task per input for non-collating tasks
        """
        mature_tasks = []
        for task in self._backlog:
            if task.callable == self._process_step or self._all_inputs_available(PMonitor.Args.get_inputs(task.args)):
                if self._constraints_fulfilled(task):
                    mature_tasks.append(task)
                    if task.callable == self._process_step:
                        input_paths = PMonitor.Args.get_inputs(task.args)
                    else:
                        input_paths = self._paths_of(PMonitor.Args.get_inputs(task.args))
                    if task.callable == self._translate_step or task.callable == self._process_step:
                        task.callable = self._process_step
                        PMonitor.Args.set_inputs(task.args, input_paths)
                        self._pool.putRequest(task)
                        if self._delay is not None:
                            time.sleep(self._delay)
                    else:
                        new_tasks = []
                        pos = self._backlog.index(task)
                        for i in range(len(input_paths)):
                            if i == 0:
                                task.callable = self._process_step
                                PMonitor.Args.set_inputs(task.args, input_paths[0:1])
                            else:
                                self._created += 1
                                args = [PMonitor.Args.get_call(task.args), self._created, PMonitor.Args.get_parameters(task.args),
                                        input_paths[i:i + 1], PMonitor.Args.get_outputs(task.args),
                                        None, PMonitor.Args.get_log_prefix(task.args), PMonitor.Args.get_async(task.args)]
                                task = WorkRequest(self._process_step, args,
                                                              priority=task.priority, requestID=self._created)
                                self._counts[PMonitor.Args.get_outputs(task.args)[0]] += 1
                            if i == 0 or self._constraints_fulfilled(task):
                                self._pool.putRequest(task)
                                if self._delay is not None:
                                    time.sleep(self._delay)
                            else:
                                new_tasks.insert(0, task)
                        if len(new_tasks) > 0:
                            for newTask in new_tasks:
                                self._backlog.insert(pos + 1, newTask)
                            if self._fair:
                                break
                else:
                    if self._fair:
                        break
        for task in mature_tasks:
            self._backlog.remove(task)

    def _constraints_fulfilled(self, request):
        """looks for host with sufficient capacity and updates host load, type load, and request host"""
        call = PMonitor.Args.get_call(request.args)
        typeConstraint = None
        for type in self._typeConstraints:
            if type.name == call:
                if type.load < type.capacity:
                    typeConstraint = type
                    break
                else:
                    return False
        weight = 1
        for type in self._weightConstraints:
            if type.name == call:
                weight = type.capacity
                break
        for host in self._hostConstraints:
            if host.load + weight <= host.capacity:
                host.load += weight
                PMonitor.Args.set_host(request.args, host.name)
                self._hostConstraints.sort()
                if typeConstraint:
                    typeConstraint.load += 1
                return True
        return False

    def _release_constraint(self, call, hostname):
        """updates host load"""
        weight = 1
        for type in self._weightConstraints:
            if type.name == call:
                weight = type.capacity
                break
        for type in self._typeConstraints:
            if type.name == call:
                type.load -= 1
                break
        for host in self._hostConstraints:
            if host.name == hostname:
                host.load -= weight
                self._hostConstraints.sort()
                if self._backlog_condition:
                    with self._backlog_condition:
                        self._backlog_condition.notify()
                return
        if self._backlog_condition:
            with self._backlog_condition:
                self._backlog_condition.notify()
        raise Exception('cannot find ' + hostname + ' in ' + str(self._hostConstraints))

    def _all_inputs_available(self, inputs):
        """
        Returns whether all inputs are bound in products map and no one waits for more non-collating tasks to contribute
        """
        for i in inputs:
            if not i in self._paths:
                return False
            if i in self._counts:
                return False
        return True

    def _mark_inputs(self, inputs):
        """
        Marks initial inputs as being bound, with the path being the same as the name
        """
        for i in inputs:
            if i not in self._paths:
                self._paths[i] = None

    def _mark_outputs(self, outputs):
        """
        Marks task outputs as being bound, with the paths preliminarily being the same as the names
        count down the count for non-collating outputs
        """
        for o in outputs:
            if o not in self._paths:
                self._paths[o] = None
            if o in self._counts:
                n = self._counts[o]
                if n <= 1:
                    self._counts.pop(o)
                else:
                    self._counts[o] = n - 1

    def _report_and_bind_outputs(self, outputs, output_paths):
        """
        Binds outputs in products map to None, a path or a set of paths, maintains report
        """
        self._mark_outputs(outputs)
        if len(output_paths) == 0:
            pass
        elif len(outputs) == len(output_paths):
            for i in range(len(outputs)):
                if output_paths[i] != outputs[i]:
                    self._report_and_bind_output(outputs[i], [output_paths[i]])
        elif len(outputs) == 1:
            self._report_and_bind_output(outputs[0], output_paths)
        else:
            raise RuntimeError('no output expected: found {1}'.format(' '.join(output_paths)))

    def _report_and_bind_output(self, output, output_paths):
        """
        Binds output in products map to set of paths, maintains report
        """
        self._bind_output(output, output_paths)
        self._report.write('#output {0} {1}\n'.format(output, ' '.join(output_paths)))

    def _bind_output(self, name, paths):
        """
        Bind output name to paths or extend existing output name by paths
        """
        if name not in self._paths or self._paths[name] is None:
            self._paths[name] = paths
        else:
            self._paths[name].extend(paths)

    def _paths_of(self, inputs):
        """
        Returns flat list of paths of the inputs, looked up in products map
        """
        if len(inputs) == 0:
            return inputs
        else:
            return reduce(lambda x, y: x + y, [self._path_of(p) for p in inputs])

    def _path_of(self, product):
        """
        Returns paths a product is bound to, or a single entry list with the product name if the product bound to its own name
        """
        paths = self._paths[product]
        if paths is None:
            return [product]
        else:
            return paths

    def _path_of_call(self, call):
        """
        Returns call prefixed with software directory for call not being an absolute path
        """
        if self._swd is not None and call[0] != '/':
            return self._swd + '/' + call
        else:
            return call
