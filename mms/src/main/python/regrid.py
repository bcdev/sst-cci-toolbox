__author__ = 'Ralf Quast'

from regridworkflow import RegridWorkflow

usecase = "regrid"
archive_root = "/neodc/esacci_sst/data/lt/"
target_root = "/group_workspaces/cems2/esacci_sst/scratch/2015_05_regridded_sst"

w = RegridWorkflow(usecase, archive_root, target_root)
w.add_sensor("ATSR1", "1991-08-01", "1997-12-31")
w.add_sensor("ATSR2", "1995-06-01", "2003-06-23")
w.add_sensor("AATSR", "2002-07-01", "2012-04-08")
w.add_sensor("AVHRR12_G", "1991-09-01", "1998-12-15")
w.add_sensor("AVHRR14_G", "1995-01-01", "2000-12-31")
w.add_sensor("AVHRR15_G", "1998-10-01", "2002-12-31")
w.add_sensor("AVHRR16_G", "2001-01-01", "2005-12-31")
w.add_sensor("AVHRR17_G", "2002-07-01", "2010-12-31")
w.add_sensor("AVHRR18_G", "2005-06-01", "2010-12-31")
w.add_sensor("AVHRRMTA_G", "2006-11-01", "2010-12-31")
w.run()
