from svr_workflow import Svr_Workflow



input_dir = "/usr/local/data/SST-CCI/*.nc"
output_dir = "/usr/local/data/delete"
sensor_name = "AVHRR"

w = Svr_Workflow(input_dir, output_dir, sensor_name)
w.run()