import glob
import svr_verify_SST_CCI_data

class Svr_Workflow:

    def __init__(self, input_dir, output_dir, sensor_name):
        """
        :param input_dir: string
        :param output_dir: string
        :param sensor_name: string
        """
        self.input_dir = input_dir
        self.output_dir = output_dir
        self.sensor_name = sensor_name

    def run(self):
        file_list = glob.glob(self.input_dir)
        print("Found " + str(len(file_list)) + " input files in '" + self.input_dir + "'")

        for input_file in file_list:
            print(input_file)
            svr_verify_SST_CCI_data.verify_SST_CCI_data(input_file, self.sensor_name, self.output_dir)
