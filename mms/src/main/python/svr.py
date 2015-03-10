from svr_workflow import Svr_Workflow
from pmonitor import PMonitor

input_dir = "/usr/local/data/SST-CCI"
output_dir = "/usr/local/data/delete"
sensor_name = "AVHRR"

class Svr:

    def __init__(self):
        self.preconditions = list()

        self.pm = PMonitor(self.preconditions, "svr")
        # enable line below to skip the real processing tb 2015-03-10
        # self.pm._simulation = True

    def run(self):
        self.pm.execute("python svr_workflow.py " + input_dir + " " + output_dir + " " + sensor_name, self.preconditions, list())
        self.pm.wait_for_completion()

if __name__ == "__main__":

    svr = Svr()
    svr.run()
