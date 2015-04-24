__author__ = 'Ralf Quast'

import unittest

from matchupverifier import MatchupVerifier


class MatchupVerifierTests(unittest.TestCase):
    def test_read_mmd(self):
        mmd, nx, ny, m_ids, m_source_filenames, m_elems, m_lines = \
            MatchupVerifier.read_mmd('atsr.3', '/Users/ralf/scratch/mms/atsr.3-mmd3-2010-06.nc')

        self.assertEquals(7, nx)
        self.assertEquals(7, ny)
        self.assertEquals(30307, len(m_ids))
        self.assertEquals(30307, len(m_source_filenames))
        self.assertEquals(30307, len(m_elems))
        self.assertEquals(30307, len(m_lines))

    def test_group_by_filename(self):
        mmd, nx, ny, m_ids, m_source_filenames, m_elems, m_lines = \
            MatchupVerifier.read_mmd('atsr.3', '/Users/ralf/scratch/mms/atsr.3-mmd3-2010-06.nc')

        grouping = MatchupVerifier.group_by_filename(m_ids, m_elems, m_lines, m_source_filenames)

        self.assertTrue('ATS_TOA_1PUUPA20100531_233726_000065272090_00001_43141_7020.N1' in grouping)
        self.assertTrue('ATS_TOA_1PUUPA20100629_114144_000065272090_00409_43549_7433.N1' in grouping)
        self.assertEquals(409, len(grouping))

        indexes = grouping['ATS_TOA_1PUUPA20100629_114144_000065272090_00409_43549_7433.N1']
        self.assertEquals(91, len(indexes))
        self.assertTrue(30306 in indexes)


if __name__ == '__main__':
    unittest.main()

