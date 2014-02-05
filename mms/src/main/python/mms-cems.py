import glob
import os
from pmonitor import PMonitor

#years = [ '2005', '2004', '2003', '2002', '2001', '2000', '1999', '1998', '1997', '1996', '1995', '1994', '1993', '1992', '1991' ]
years  = [ '2002', '2003' ]
#months = [ '12', '11', '10', '09', '08', '07', '06', '05', '04', '03', '02', '01' ]
months = [ '01', '02', '12' ]

inputs = []
for year in years:
    for month in months:
        if (year == '2002' and month < '12') or (year == '2003' and month > '02'):
            continue
        inputs.append('/stg/'+year+'/'+month)

hosts  = [('localhost',12)]
types  = [('staging-start.sh',4),
          ('ingestion-start.sh',4),
          ('ingestion-run.sh',4),
          ('sampling-run.sh',1),
          ('mapplot-run.sh',1),
          ('matchup2-start.sh',4),
          ('arc12-start.sh',4),
          ('reingest12-start.sh',1),
          ('nwparc3-start.sh',3),
          ('nwpmatchup-start.sh',1),
          ('reingest3-start.sh',1),
          ('mmd-start.sh',8),
          ('rrdptest-start.sh',4),
          ('rrdpalgsel-start.sh',4),
          ('destaging-start.sh',2)]

# keep request name constant to re-use report etc.

pm = PMonitor(inputs,
              request='mms',
              logdir='log',
              hosts=hosts,
              types=types)

def prev_month_year_of(year, month):
        if month == '02':
            return year, '01'
        elif month == '03':
            return year, '02'
        elif month == '04':
            return year, '03'
        elif month == '05':
            return year, '04'
        elif month == '06':
            return year, '05'
        elif month == '07':
            return year, '06'
        elif month == '08':
            return year, '07'
        elif month == '09':
            return year, '08'
        elif month == '10':
            return year, '09'
        elif month == '11':
            return year, '10'
        elif month == '12':
            return year, '11'
        else:
            return str(int(year) - 1), '12'

def next_month_year_of(year, month):
        if month == '01':
            return year, '02'
        elif month == '02':
            return year, '03'
        elif month == '03':
            return year, '04'
        elif month == '04':
            return year, '05'
        elif month == '05':
            return year, '06'
        elif month == '06':
            return year, '07'
        elif month == '07':
            return year, '08'
        elif month == '08':
            return year, '09'
        elif month == '09':
            return year, '10'
        elif month == '10':
            return year, '11'
        elif month == '11':
            return year, '12'
        else:
            return str(int(year) + 1), '01'

for year in years:
    prev_year = str(int(year) - 2)
    next_year = str(int(year) + 1)
    for month in months:
        if (year == '2002' and month < '12') or (year == '2003' and month > '02'):
            continue
        prev_month_year, prev_month = prev_month_year_of(year, month)
        next_month_year, next_month = next_month_year_of(year, month)
        #pm.execute('staging-start.sh', ['/nas/'+year], ['/stg/'+year+'/'+month], parameters=[year, month])
        #pm.execute('ingestion-start.sh', ['/stg/'+year+'/'+month], ['/ing/'+year+'/'+month], parameters=[year, month])
        pm.execute('ingestion-run.sh', ['/stg/'+year+'/'+month], ['/ing/'+year+'/'+month], parameters=[year, month])
        # TODO implement incremental global sampling
        if year == '2003' and month == '01':
            pm.execute('sampling-run.sh', [ '/ing/2002/12', '/ing/2003/01', '/ing/2003/02' ], [ '/sam/2003/01' ], parameters=[year, month])
            pm.execute('mapplot-run.sh', [ '/sam/2003/01' ], [ '/plo/2003/01' ], parameters=[year, month])
	continue
        pm.execute('matchup2-start.sh',
                   ['/ing/'+year+'/'+month, '/ing/'+prev_month_year+'/'+prev_month, '/ing/'+next_month_year+'/'+next_month],
                   ['/ma2/'+year+'/'+month], parameters=[year, month])
        pm.execute('arc12-start.sh', ['/ma2/'+year+'/'+month], ['/a12/'+year+'/'+month], parameters=[year, month])
        pm.execute('reingest12-start.sh', ['/a12/'+year+'/'+month], ['/r12/'+year+'/'+month], parameters=[year, month])
        pm.execute('nwparc3-start.sh', ['/r12/'+year+'/'+month], ['/ar3/'+year+'/'+month], parameters=[year, month])
        pm.execute('nwpmatchup-start.sh', ['/ma2/'+year+'/'+month], ['/nwp/'+year+'/'+month], parameters=[year, month])
        pm.execute('reingest3-start.sh', ['/ar3/'+year+'/'+month, '/nwp/'+year+'/'+month],
                                   ['/re3/'+year+'/'+month], parameters=[year, month])
        pm.execute('mmd-start.sh', ['/re3/'+year+'/'+month], ['/mmd/'+year+'/'+month], parameters=[year, month])
#        pm.execute('rrdptest-start.sh', ['/re3/'+year+'/'+month], ['/rrd/'+year+'/'+month], parameters=[year, month])
#        pm.execute('rrdpalgsel-start.sh', ['/re3/'+year+'/'+month], ['/alg/'+year+'/'+month], parameters=[year, month])
#        pm.execute('destaging-start.sh', ['/mmd/'+year+'/'+month, '/rrd/'+year+'/'+month, '/alg/'+year+'/'+month ],
#                                         ['/nas/'+prev_year], parameters=[year, month])
        pm.execute('destaging-start.sh', ['/mmd/'+year+'/'+month ], ['/nas/'+prev_year], parameters=[year, month])

pm.wait_for_completion()
