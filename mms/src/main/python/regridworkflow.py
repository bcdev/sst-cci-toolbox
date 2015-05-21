__author__ = "Gerrit Holl"

import svrworkflow
import workflow

class RegridWorkflow(svrworkflow.SvrWorkflow):
    def __init__(self, usecase, archive_root, target_root,
            regrid_period=None):
        self.usecase = usecase
        self.archive_root = archive_root
        self.target_root = target_root
        self.sensors = set()
        self.regrid_period = regrid_period

    def get_regrid_period(self):
        return self.regrid_period

    def run(self, hosts=[("localhost", 120)], calls=[], log_dir="trace", simulation=False):

        # Cannot run self.__get_monitor because name mangling makes
        # inheritance a pain.  Instead do manually what
        # SvrWorkflow.__get_monitor does really.
        m = workflow.Monitor([], self.get_usecase(), hosts, calls, log_dir, simulation)

        production_period = self.__get_effective_regrid_period()
        date = production_period.get_start_date()

        while date < production_period.get_end_date():
            chunk = workflow.Period(date, svrworkflow._next_year_start(date))
            self._execute_regrid(m, chunk)
            date = svrworkflow._next_year_start(date)
        m.wait_for_completion_and_terminate()

    def __get_effective_regrid_period(self):
        data_period = self._get_data_period()
        if data_period is None:
            return None
        regrid_period = self.get_regrid_period()
        if regrid_period is None:
            return data_period
        else:
            return regrid_period.get_intersection(data_period)

    def _execute_regrid(self, monitor, chunk):
        """

        :type monitor: Monitor
        :type chunk: Period
        """
        for sensor in self._get_sensors_by_period():
            sensor_name = sensor.get_name()
            period = sensor.get_period().get_intersection(chunk)
            if period is not None:
                start_date = period.get_start_date()
                end_date = period.get_end_date()
                date = start_date
                while date < end_date:
                    (year, month, day) = svrworkflow._year_month_day(date)
                    if int(day) != 1:
                        raise ValueError(("Gridding month must start on day 1, "
                            "got {}-{}-{}").format(year, month, day))
                    job = workflow.Job(
                        "regrid-start-{year}-{month}-{sensor_name}".format(**vars()),
                        "regrid-start.sh",
                        [],
                        ["/regrid/{sensor_name}".format(**vars())],
                        [year, month, sensor_name])
                    monitor.execute(job)
                    date = svrworkflow._next_month(date)
