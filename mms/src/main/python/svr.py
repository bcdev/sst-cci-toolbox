from pmonitor import PMonitor
import os

# input from "config"
python_path = "python"
archive_root = "/usr/local/data/SST-CCI"
# varying inout (cmd-line?)
sensor_name = "AVHRRMTA_G"
year = "2008"
month = "05"
output_dir = "/usr/local/data/delete"



class Svr:
    def __init__(self):
        self.preconditions = list()

        self.pm = PMonitor(self.preconditions, "svr")
        # enable line below to skip the real processing tb 2015-03-10
        # self.pm._simulation = True

    def run(self):
        input_path = self.assemble_input_path(archive_root, sensor_name, year, month)
        call_sensor_name = self.extract_sensor_name(sensor_name)
        call = self.assemble_call(python_path, input_path, output_dir, call_sensor_name)

        self.pm.execute(call, self.preconditions, list())
        self.pm.wait_for_completion()

    def assemble_call(self, python_path, input_dir, output_dir, sensor_name):
        return python_path + " svr_workflow.py " + input_dir + " " + output_dir + " " + sensor_name

    def assemble_input_path(self, archive_root, sensor, year, month):
        sensor_path = os.path.join(archive_root, sensor, year, month)
        return sensor_path

    def extract_sensor_name(self, sensor_name):
        sensor_name_uc = sensor_name.upper()

        if "AVHRR" in sensor_name_uc:
            return "AVHRR"
        elif "ATSR" in sensor_name_uc:
            return "ATSR"

        return ""



if __name__ == "__main__":
    svr = Svr()
    svr.run()
