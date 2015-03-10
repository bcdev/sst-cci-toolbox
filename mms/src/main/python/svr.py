from pmonitor import PMonitor

python_path = "python"
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
        call = self.assemble_call(python_path, input_dir, output_dir, sensor_name)
        self.pm.execute(call, self.preconditions, list())
        self.pm.wait_for_completion()

    def assemble_call(self, python_path, input_dir, output_dir, sensor_name):
        return python_path + " svr_workflow.py " + input_dir + " " + output_dir + " " + sensor_name


if __name__ == "__main__":
    svr = Svr()
    svr.run()
