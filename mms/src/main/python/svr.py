from svr_workflow import Svr_Workflow

input_dir = "C:\Data\SST-CCI\*.nc"
output_dir = "C:\Data\delete"
sensor_name = "AVHRR"

w = Svr_Workflow(input_dir, output_dir, sensor_name)
w.run()