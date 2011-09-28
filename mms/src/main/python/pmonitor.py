import glob
import os
import sys
import threading
import threadpool
import subprocess

__author__ = 'boe'

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
        def __init__(self,name,capacity):
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

    _pool = None
    _backlog = []
    _running = []
    _failed = []
    _created = 0
    _processed = 0
    _report = None
    _status = None
    _commands = set([])
    _paths = dict([])
    _counts = dict([])
    _hostConstraints = []
    _typeConstraints = []
    _swd = None
    _logdir = '.'
    _cache = None
    _mutex = threading.Lock()
    _request = None
    _simulation = False

    def __init__(self, inputs, request='', hosts=[('localhost',4)], types=[], swd=None, cache=None, logdir='.', simulation=False):
        """
        Initiates monitor, marks inputs, reads report, creates thread pool
        """
        try:
            os.system('mkdir -p ' + logdir)     
            self._mutex.acquire()
            self._hostConstraints = self._constraints_of(hosts)
            self._typeConstraints = self._constraints_of(types)
            self._swd = swd
            self._logdir = logdir
            self._cache = cache
            self._mark_inputs(inputs)
            self._maybe_read_report(request + '.report')
            self._status = open(request + '.status', 'w')
            concurrency = sum(map(lambda host:host[1], hosts))
            self._pool = threadpool.ThreadPool(concurrency)
            self._request = request
            self._simulation = simulation
        finally:
            self._mutex.release()

    def execute(self, call, inputs, outputs, parameters=[], priority=1, collating=True):
        """
        Schedules task `call parameters inputs outputs`, either a single collating call or one call per input
        """
        try:
            self._mutex.acquire()
            if self._all_inputs_available(inputs) and self._type_constraints_fulfilled(call):
                inputPaths = self._paths_of(inputs)
                if collating:
                    self._created += 1
                    request = threadpool.WorkRequest(self._process_step, [call, self._created, parameters, inputPaths, outputs], priority=priority)
                    self._pool.putRequest(request)
                else:
                    self._counts[outputs[0]] = len(inputPaths)
                    for i in range(len(inputPaths)):
                        if i == 0 or self._type_constraints_fulfilled(call):
                            self._created += 1
                            request = threadpool.WorkRequest(self._process_noncollating_step, [call, self._created, parameters, inputPaths[i:i+1], outputs], priority=priority)
                            self._pool.putRequest(request)
                        else:
                            self._created += 1
                            request = threadpool.WorkRequest(self._process_noncollating_step, [call, self._created, parameters, inputs[i:i+1], outputs], priority=priority)
                            self._backlog.append(request)
            else:
                if collating:
                    handler = self._process_step
                else:
                    handler = self._process_noncollating_step
                self._created += 1
                request = threadpool.WorkRequest(handler, [call, self._created, parameters, inputs, outputs], priority=priority)
                self._backlog.append(request)
        finally:
            self._mutex.release()

    def wait_for_completion(self):
        """
        Waits until all scheduled tasks are run, then returns
        """
        self._write_status(withBacklog=False)
        while True:
            self._pool.wait()
            try:
                self._mutex.acquire()
                if not self._pool.workRequests:
                    break
            finally:
                self._mutex.release()
        self._write_status(withBacklog=True)
        self._status.close()
        return int(bool(self._failed or self._backlog))


    def _constraints_of(self, config):
        """
        Converts configuration into list of constraints
        """
        constraints = map(lambda (name,limit): PMonitor.Constraint(name, limit), config)
        constraints.sort()
        return constraints

    def _maybe_read_report(self, reportPath):
        """
        Reads report containing lines with command line calls and lines with an output path of a product name, e.g.
          bin/meris-l3.sh /home/boe/eodata/MER_WV__2P/v01/2010/01/25 /home/boe/eodata/MER_WV__3P/v01/2010/01/25/meris-l3-daily-25.dat
          #output /home/boe/eodata/MER_WV__3P/v01/2010/01/25/meris-l3-daily-25.dat /home/boe/eodata/MER_WV__3P/v01/2010/01/25/meris-l3-daily-25.dat.real
        """
        if glob.glob(reportPath):
            self._report = open(reportPath, 'r+')
            for line in self._report.readlines():
                if line.startswith('#output '):
                    w = line.split()
                    name = w[1]
                    paths = w[2:]
                    self._bind_output(name, paths)
                else:
                    self._commands.add(line[:-1])
        else:
            self._report = open(reportPath, 'w')

    def _write_status(self, withBacklog=False):
        self._status.seek(0)
        pending = len(self._pool.workRequests) - len(self._running)
        self._status.write('{0} created, {1} running, {2} pending, {3} backlog, {4} processed, {5} failed\n'.\
        format(self._created, len(self._running), pending, len(self._backlog), self._processed, len(self._failed)))
        for l in self._failed:
            self._status.write('f {0}\n'.format(l))
        for l in self._running:
            self._status.write('r {0}\n'.format(l))
        if withBacklog:
            for r in self._backlog:
                self._status.write('b {0} {1} {2} {3}\n'.format(r.args[0], ' '.join(r.args[2]), ' '.join(r.args[3]), ' '.join(r.args[4])))
        self._status.truncate()
        self._status.flush()

    def _process_noncollating_step(self, call, taskId, parameters, inputs, outputs):
        """
        Callable for tasks, marker for non-collating tasks, same implementation as _process_step
        """
        self._process_step(call, taskId, parameters, inputs, outputs)

    def _process_step(self, call, taskId, parameters, inputs, outputs):
        """
        Callable for tasks, marker for simple or collating tasks.
        looks up call in commands from report, maybe skips execution
        executes call by os call, scans process stdout for `output=...`
        updates report, maintains products, and schedule mature tasks
        """
        command = '{0} {1} {2} {3}'.format(self._path_of_call(call), ' '.join(parameters), ' '.join(inputs), ' '.join(outputs))
        if command in self._commands:
            self._skip_step(call, command, outputs)
        else:
            host = self._prepare_step(command)
            outputPaths = []
            if not self._simulation:
                code = self._run_step(taskId, host, command, outputPaths)
            else:
                code = 1
            self._finalise_step(call, code, command, host, outputPaths, outputs)

    def _skip_step(self, call, command, outputs):
        """
        Marks outputs of step and schedules mature tasks
        """
        try:
            self._mutex.acquire()
            sys.__stdout__.write('skipping {0}\n'.format(command))
            self._release_type(call)
            self._mark_outputs(outputs)
            self._check_for_mature_tasks()
            self._processed += 1
        finally:
            self._mutex.release()

    def _prepare_step(self, command):
        """
        Updates status, selects host and returns it
        """
        try:
            self._mutex.acquire()
            self._running.append(command)
            self._write_status()
            return self._select_host()
        finally:
            self._mutex.release()

    def _run_step(self, taskId, host, command, outputPaths):
        """
        Executes command on host, collects output paths if any, returns exit code
        """
        wd = self._prepare_working_dir(taskId)
        process = self._start_processor(command, host, wd)
        self._trace_processor_output(outputPaths, process, taskId, wd)
        process.stdout.close()
        code = process.wait()
        if code == 0 and self._cache != None and 'cache' in wd:
            subprocess.call(['rm', '-rf', wd])
        return code

    def _finalise_step(self, call, code, command, host, outputPaths, outputs):
        """
        releases host and type resources, updates report, schedules mature steps, handles failure
        """
        try:
            self._mutex.acquire()
            self._release_host(host)
            self._release_type(call)
            self._running.remove(command)
            if code == 0:
                self._report.write(command + '\n')
                self._report_and_bind_outputs(outputs, outputPaths)
                self._report.flush()
                self._processed += 1
            else:
                self._failed.append(command)
                sys.__stderr__.write('failed {0}\n'.format(command))
            self._check_for_mature_tasks()
            self._write_status()
        finally:
            self._mutex.release()

    def _prepare_working_dir(self, taskId):
        """Creates working directory in .../cache/request-id/task-id"""
        if self._cache == None:
            wd = '.'
        else:
            wd = self._cache + '/' + self._request + '/' + '{0:04d}'.format(taskId)
        if not os.path.exists(wd):
            os.makedirs(wd)
        return wd

    def _start_processor(self, command, host, wd):
        """starts subprocess either locally or via ssh, returns process handle"""
        if host == 'localhost':
            cmd = command
            sys.__stdout__.write('executing {0}'.format(cmd + '\n'))
            process = subprocess.Popen(cmd, shell=True, bufsize=1, cwd=wd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        else:
            cmd = 'ssh ' + host + " 'mkdir -p " + wd + ';' + " cd " + wd + ';' + command + "'"
            sys.__stdout__.write('executing {0}'.format(cmd + '\n'))
            process = subprocess.Popen(cmd, shell=True, bufsize=1, cwd=wd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        return process

    def _trace_processor_output(self, outputPaths, process, taskId, wd):
        """traces processor output, recognises 'output=' lines, writes all lines to trace file in working dir"""
        if self._cache == None:
            trace = open(self._logdir + '/' + self._request + '-' + '{0:04d}'.format(taskId) + '.stdout', 'w')
        else:
            trace = open(wd + '/' + self._request + '-' + '{0:04d}'.format(taskId) + '.stdout', 'w')
        for line in process.stdout:
            if line.startswith('output='):
                outputPaths.append(line[7:].strip())
            trace.write(line)
            trace.flush()
        trace.close()

    def _check_for_mature_tasks(self):
        """
        Checks tasks in backlog whether inputs are (now) all bound in products map
        adds these mature tasks to thread pool, removes them from backlog
        distinguishes collocating and non-collocating asks by the callable used
        generates one task per input for non-collocating tasks
        """
        matureTasks = []
        for task in self._backlog:
            if self._all_inputs_available(task.args[3]) and self._type_constraints_fulfilled(task.args[0]):
                inputPaths = self._paths_of(task.args[3])
                if task.callable == self._process_step:
                    task.args[3] = inputPaths
                    self._pool.putRequest(task)
                    matureTasks.append(task)
                else:
                    self._counts[task.args[4][0]] = len(inputPaths)
                    for i in range(len(task.args[3])):
                        if i == 0 or self._type_constraints_fulfilled(task.args[0]):
                            self._created += 1
                            request = threadpool.WorkRequest(self._process_noncollating_step,\
                                [task.args[0], self._created, task.args[2], inputPaths[i:i+1], task.args[4]],\
                                                             priority=task.priority)
                            self._pool.putRequest(request)
                        else:
                            self._created += 1
                            request = threadpool.WorkRequest(self._process_noncollating_step,\
                                [task.args[0], self._created, task.args[2], task.args[3][i:i+1], task.args[4]],\
                                                             priority=task.priority)
                            self._backlog.append(request)
                    matureTasks.append(task)
        for task in matureTasks:
            self._backlog.remove(task)



    def _select_host(self):
        """returns host with idle capacity and lowest load"""
        try:
            for host in self._hostConstraints:
                if host.load < host.capacity:
                    host.load += 1
                    return host.name
            self._hostConstraints[0].load += 1
            return self._hostConstraints[0].name
        finally:
            self._hostConstraints.sort()

    def _release_host(self, usedHost):
        """decrements load of host just released"""
        try:
            for host in self._hostConstraints:
                if host.name == usedHost:
                    host.load -= 1
                    return
            raise Exception('cannot find ' + usedHost + ' in ' + str(self._hostConstraints))
        finally:
            self._hostConstraints.sort()

    def _type_constraints_fulfilled(self, name):
        for i in self._typeConstraints:
            if i.name == name:
                if i.load < i.capacity:
                    i.load += 1
                    return i
                else:
                    return False
        return True

    def _release_type(self, name):
        for i in self._typeConstraints:
            if i.name == name:
                i.load -= 1
                break

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

    def _report_and_bind_outputs(self, outputs, outputPaths):
        """
        Binds outputs in products map to None, a path or a set of paths, maintains report
        """
        self._mark_outputs(outputs)
        if len(outputPaths) == 0:
            pass
        elif len(outputs) == len(outputPaths):
            for i in range(len(outputs)):
                if outputPaths[i] != outputs[i]:
                    self._report_and_bind_output(outputs[i], [outputPaths[i]])
        elif len(outputs) == 1:
            self._report_and_bind_output(outputs[0], outputPaths)
        else:
            raise RuntimeError('no output expected: found {1}'.format(' '.join(outputPaths)))

    def _report_and_bind_output(self, output, outputPaths):
        """
        Binds output in products map to set of paths, maintains report
        """
        self._bind_output(output, outputPaths)
        self._report.write('#output {0} {1}\n'.format(output, ' '.join(outputPaths)))

    def _bind_output(self, name, paths):
        """
        Bind output name to paths or extend existing output name by paths
        """
        if name not in self._paths or self._paths[name] == None:
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
            return reduce(lambda x, y: x + y, map(lambda p: self._path_of(p), inputs))

    def _path_of(self, product):
        """
        Returns paths a product is bound to, or a single entry list with the product name if the product bound to its own name
        """
        paths = self._paths[product]
        if paths == None:
            return [product]
        else:
            return paths

    def _path_of_call(self, call):
        if self._swd != None and call[0] != '/':
            return self._swd + '/' + call
        else:
            return call
