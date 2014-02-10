from pmonitor import PMonitor

usecase='mms-wpr-02'

years = ['1991', '1992', '1993', '1994', '1995', '1996', '1997', '1998', '1999', '2000',
         '2001', '2002', '2003', '2004', '2005', '2006', '2007', '2008', '2009', '2010',
         '2011', '2012']
years = ['2002', '2003']
months = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']
sensors = {('atsr.1', '1991-01-17', '1998-07-15'), ('atsr.2', '1998-02', '2003-03'), ('atsr.3', '2002-04', '2012-04')}
samplespermonth = 300000

# archiving rules
# mms/archive/atsr.3/v2.1/2003/01/17/ATS_TOA_1P...N1
# mms/archive/mms2/smp/atsr.3/2003/atsr.3-smp-2003-01-b.txt
# mms/archive/mms2/sub/atsr.3/2003/atsr.3-sub-2003-01.nc
# mms/archive/mms2/nwp/atsr.3/2003/atsr.3-nwp-2003-01.nc
# mms/archive/mms2/nwp/atsr.3/2003/atsr.3-nwpAn-2003-01.nc
# mms/archive/mms2/nwp/atsr.3/2003/atsr.3-nwpFc-2003-01.nc
# mms/archive/mms2/arc/atsr.3/2003/atsr.3-arc-2003-01.nc
# mms/archive/mms2/mmd/atsr.3/2003/atsr.3-mmd-2003-01.nc

# TODO consider exact dates of start and end of mission for sampling

def prev_year_month_of(year, month):
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


def next_year_month_of(year, month):
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


inputs = []
for year in years:
    for month in months:
        for sensor, _, __ in sensors:
            inputs.append('/inp/' + sensor + '/' + year + '/' + month)
for (sensor, sensorstart, sensorstop) in sensors:
    if years[0] + '-' + months[0] >= sensorstart:
        prev_month_year, prev_month = prev_year_month_of(years[0], months[0])
    else:
        prev_month_year, prev_month = prev_year_month_of(sensorstart[0:4], sensorstart[5:7])
    inputs.append('/obs/' + prev_month_year + '/' + prev_month)
    if years[-1] + '-' + months[-1] <= sensorstop:
        next_month_year, next_month = next_year_month_of(years[-1], months[-1])
    else:
        next_month_year, next_month = next_year_month_of(sensorstop[0:4], sensorstop[5:7])
    inputs.append('/obs/' + next_month_year + '/' + next_month)

hosts = [('localhost', 1)]
types = [('ingestion-run2.sh', 12),
         ('sampling-run2.sh', 12),
         ('clearsky-run2.sh', 12),
         ('mmd-run2.sh', 12),
         ('coincidence-run2.sh', 12),
         ('nwp-run2.sh', 12),
         ('arc-run2.sh', 12),
         ('reingestion-run2.sh', 12)]

pm = PMonitor(inputs,
              request=usecase,
              logdir='log',
              hosts=hosts,
              types=types)

for year in years:
    for month in months:
        pm.execute('ingestion-run2.sh', ['/inp/' + year + '/' + month], ['/obs/' + year + '/' + month],
                   parameters=[year, month, usecase])
        for sensor, sensorstart, sensorstop in sensors:
            if year + '-' + month < sensorstart or year + '-' + month > sensorstop:
                continue
            prev_month_year, prev_month = prev_year_month_of(year, month)
            next_month_year, next_month = next_year_month_of(year, month)
            pm.execute('sampling-run2.sh',
                       ['/obs/' + prev_month_year + '/' + prev_month, '/obs/' + year + '/' + month,
                        '/obs/' + next_month_year + '/' + next_month],
                       ['/smp/' + sensor + '/' + prev_month_year + '/' + prev_month,
                        '/smp/' + sensor + '/' + year + '/' + month,
                        '/smp/' + sensor + '/' + next_month_year + '/' + next_month],
                       parameters=[year, month, sensor, samplespermonth, usecase])
            pm.execute('clearsky-run2.sh', ['/smp/' + sensor + '/' + year + '/' + month],
                       ['/clr/' + sensor + '/' + year + '/' + month], parameters=[year, month, sensor, usecase])
            pm.execute('mmd-run2.sh', ['/clr/' + sensor + '/' + year + '/' + month],
                       ['/sub/' + sensor + '/' + year + '/' + month], parameters=[year, month, sensor, 'sub', usecase])
            pm.execute('coincidence-run2.sh', ['/clr/' + sensor + '/' + year + '/' + month],
                       ['/con/' + sensor + '/' + year + '/' + month], parameters=[year, month, sensor, usecase])
            pm.execute('nwp-run2.sh', ['/sub/' + sensor + '/' + year + '/' + month],
                       ['/nwp/' + sensor + '/' + year + '/' + month], parameters=[year, month, sensor, usecase])
            pm.execute('nwpmatchup-run2.sh', ['/sub/' + sensor + '/' + year + '/' + month],
                       ['/nwp/' + sensor + '/' + year + '/' + month], parameters=[year, month, sensor, usecase])
            pm.execute('arc-run2.sh', ['/nwp/' + sensor + '/' + year + '/' + month],
                       ['/arc/' + sensor + '/' + year + '/' + month], parameters=[year, month, sensor, usecase])
            pm.execute('reingestion-run2.sh', ['/sub/' + sensor + '/' + year + '/' + month],
                       ['/con/' + sensor + '/' + year + '/' + month], parameters=[year, month, sensor, 'sub', usecase])
            pm.execute('reingestion-run2.sh', ['/nwp/' + sensor + '/' + year + '/' + month],
                       ['/con/' + sensor + '/' + year + '/' + month], parameters=[year, month, sensor, 'nwp', usecase])
            pm.execute('reingestion-run2.sh', ['/arc/' + sensor + '/' + year + '/' + month],
                       ['/con/' + sensor + '/' + year + '/' + month], parameters=[year, month, sensor, 'arc', usecase])
            pm.execute('mmd-run2.sh', ['/con/' + sensor + '/' + year + '/' + month],
                       ['/mmd/' + sensor + '/' + year + '/' + month], parameters=[year, month, sensor, 'mmd2', usecase])

pm.wait_for_completion()
