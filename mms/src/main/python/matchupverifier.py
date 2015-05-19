__author__ = 'Ralf Quast'

from netCDF4 import chartostring
from netCDF4 import Dataset
import epr
import numpy
import os


class MatchupVerifier:
    def __init__(self, sensor, mmd_pathname, product_dirpath):
        """

        :type sensor: str
        :type mmd_pathname: str
        """
        self.sensor = sensor
        self.mmd_pathname = mmd_pathname
        self.product_dirpath = product_dirpath

    def verify(self):
        mmd, nx, ny, m_ids, m_source_filenames, m_elems, m_lines = MatchupVerifier.read_mmd(self.sensor, self.mmd_pathname)

        grouping = self.group_by_filename(m_ids, m_elems, m_lines, m_source_filenames)

        for filename, indexes in sorted(grouping.iteritems()):
            dataset = MatchupVerifier.read_dataset(os.path.join(self.product_dirpath, filename))


    @staticmethod
    def group_by_filename(m_ids, m_elems, m_lines, m_source_filenames):
        """

        :type m_ids: numpy.ndarray
        :type m_elems: numpy.ndarray
        :type m_lines: numpy.ndarray
        :type m_source_filenames: numpy.ndarray
        :rtype : dict
        """
        grouping = {}
        for i in range(len(m_ids)):
            m_elem = m_elems[i]
            m_line = m_lines[i]
            if m_elem >= 0 and m_line >= 0:
                m_source_filename = m_source_filenames[i]
                if m_source_filename not in grouping:
                    grouping[m_source_filename] = list()
                grouping[m_source_filename].append(i)
        return grouping

    @staticmethod
    def read_mmd(sensor, pathname):
        """

        :type sensor: str
        :type pathname: str
        :rtype : tuple
        """
        mmd = Dataset(pathname)
        mmd.set_auto_maskandscale(True)
        nx = len(mmd.dimensions['atsr.nx'])
        ny = len(mmd.dimensions['atsr.ny'])
        m_ids = mmd.variables['matchup.id'][:]
        mmd.variables[sensor + '.matchup_elem'].set_auto_maskandscale(False)
        mmd.variables[sensor + '.matchup_line'].set_auto_maskandscale(False)
        m_elems = mmd.variables['atsr.3.matchup_elem'][:]
        m_lines = mmd.variables['atsr.3.matchup_line'][:]
        m_source_filenames = chartostring(mmd.variables[sensor + '.l1b_filename'][:])

        return mmd, nx, ny, m_ids, m_source_filenames, m_elems, m_lines

    @staticmethod
    def read_dataset(pathname):
        """

        :type pathname: str
        :rtype : Dataset
        """
        dataset = Dataset(pathname)
        dataset.set_auto_maskandscale(True)

        return dataset

