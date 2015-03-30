import os

from svr_product_verifier import ProductVerifier


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

        file_list = self.glob_input_files()

        print("Found " + str(len(file_list)) + " input files in '" + self.input_path + "'")

        for source_pathname in file_list:
            print(source_pathname)
            report_filename = os.path.basename(source_pathname) + '.json'
            report_pathname = os.path.join(self.output_dir, report_filename)
            verifier = ProductVerifier(source_pathname, report_pathname)
            verifier.verify()

    def glob_input_files(self):
        input_file_list = list()
        # TODO - find out how to use sensor name here
        for root, dirs, files in os.walk(self.input_path):
            for name in files:
                if name.endswith(".nc"):
                    path = os.path.join(root, name)
                    input_file_list.append(os.path.abspath(path))

        return input_file_list


if __name__ == "__main__":
    import sys

    w = Svr_Workflow(sys.argv[1], sys.argv[2], sys.argv[3])
    w.run()