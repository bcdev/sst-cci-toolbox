import os
import time

import svr_verify_SST_CCI_data


class Svr_Workflow:
    def __init__(self, input_path, output_dir, sensor_name):
        """
        :param input_path: string
        :param output_dir: string
        :param sensor_name: string
        """
        self.input_path = input_path
        self.output_dir = output_dir
        self.sensor_name = sensor_name

    def run(self):
        #start_time = time.clock()

        file_list = self.glob_input_files()

        print("Found " + str(len(file_list)) + " input files in '" + self.input_path + "'")

        for input_file in file_list:
            print(input_file)
            svr_verify_SST_CCI_data.verify_SST_CCI_data(input_file, self.sensor_name, self.output_dir)

        #stop_time = time.clock()
        #print(stop_time - start_time)

    def glob_input_files(self):
        input_file_list = list()

        for root, dirs, files in os.walk(self.input_path):
            for name in files:
                if name.endswith(".nc"):
                    path = os.path.join(root,name)
                    input_file_list.append(os.path.abspath(path))

        return input_file_list


if __name__ == "__main__":
    import sys


    w = Svr_Workflow(sys.argv[1], sys.argv[2], sys.argv[3])
    w.run()