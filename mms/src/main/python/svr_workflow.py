import glob
import svr_verify_SST_CCI_data

class Svr_Workflow:

    def __init__(self, input_wildcard, output_dir, sensor_name):
        """
        :param input_wildcard: string
        :param output_dir: string
        :param sensor_name: string
        """
        self.input_wildcard = input_wildcard
        self.output_dir = output_dir
        self.sensor_name = sensor_name

    def run(self):
        file_list = glob.glob(self.input_wildcard)
        print("Found " + str(len(file_list)) + " input files in '" + self.input_wildcard + "'")

        for input_file in file_list:
            print(input_file)
            svr_verify_SST_CCI_data.verify_SST_CCI_data(input_file, self.sensor_name, self.output_dir)

if __name__ == "__main__":
    import sys

    input_wildcard = sys.argv[1]
    if input_wildcard[-1] == '/':
        input_wildcard = sys.argv[1][0:-1]
    input_wildcard += "/*.nc"

    w = Svr_Workflow(input_wildcard, sys.argv[2], sys.argv[3])
    w.run()