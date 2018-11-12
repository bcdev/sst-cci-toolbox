import os
import unittest
from io import StringIO

from lib.jasmin.jasmin_job_monitor import JasminJobMonitor
from lib.status_codes import StatusCodes


class JasminJobMonitorTest(unittest.TestCase):

    def setUp(self):
        self._monitor = JasminJobMonitor()

    def test_parse_bjobs_no_jobs(self):
        message = "No unfinished job found"

        status_dict = self._monitor._parse_bjobs_call(message)
        self.assertEqual(0, len(status_dict))

    def test_parse_bjobs_one_job_running(self):
        message = "JOBID     USER    STAT  QUEUE      FROM_HOST   EXEC_HOST   JOB_NAME   SUBMIT_TIME\n" \
                  "7254358   tblock0 RUN   short-seri cems-sci1-p host326.jc. test_bc_6  Oct 25 09:29\n"

        status_dict = self._monitor._parse_bjobs_call(message)
        self.assertEqual(1, len(status_dict))
        self.assertEqual(StatusCodes.RUNNING, status_dict["7254358"])

    def test_parse_bjobs_mixed_case(self):
        # example from Ralf tb 2018-10-25
        message = "JOBID     USER    STAT  QUEUE      FROM_HOST   EXEC_HOST   JOB_NAME   SUBMIT_TIME\n" \
                  "619450  rquast  RUN    lotus      lotus.jc.rl host042.jc. *r.n10-sub Aug 14 10:15\n" \
                  "619464  rquast  PEND   lotus      lotus.jc.rl host087.jc. *r.n11-sub Aug 14 10:15\n" \
                  "619457  rquast  PEND   lotus      lotus.jc.rl host209.jc. *r.n12-sub Aug 14 10:15\n" \
                  "619458  rquast  RUN    lotus      lotus.jc.rl host209.jc. *r.n11-sub Aug 14 10:15\n" \
                  "619452  rquast  RUN    lotus      lotus.jc.rl host043.jc. *r.n10-sub Aug 14 10:15\n"

        status_dict = self._monitor._parse_bjobs_call(message)
        self.assertEqual(5, len(status_dict))
        self.assertEqual(StatusCodes.RUNNING, status_dict["619450"])
        self.assertEqual(StatusCodes.SCHEDULED, status_dict["619464"])

    def test_extract_id_and_status(self):
        line = "7254358   tblock0 RUN   short-seri cems-sci1-p host326.jc. test_bc_6  Oct 25 09:29"

        id_status = self._monitor._extract_id_and_status(line)
        self.assertEqual({"7254358": StatusCodes.RUNNING}, id_status)

    def test_status_to_enum(self):
        self.assertEqual(StatusCodes.RUNNING, self._monitor._status_to_enum("RUN"))
        self.assertEqual(StatusCodes.SCHEDULED, self._monitor._status_to_enum("PEND"))
        self.assertEqual(StatusCodes.UNKNOWN, self._monitor._status_to_enum("UNKNOWN"))
        self.assertEqual(StatusCodes.DONE, self._monitor._status_to_enum("DONE"))
        self.assertEqual(StatusCodes.FAILED, self._monitor._status_to_enum("EXIT"))
        self.assertEqual(StatusCodes.DROPPED, self._monitor._status_to_enum("PSUSP"))
        self.assertEqual(StatusCodes.DROPPED, self._monitor._status_to_enum("USUSP"))
        self.assertEqual(StatusCodes.DROPPED, self._monitor._status_to_enum("SSUSP"))

    def test_parse_watch_dict_empty(self):
        self.assertEqual({}, self._monitor._parse_watch_dict(""))

    def test_parse_watch_dict_one_job(self):
        self.assertEqual({"77865342": "ingest_AIRS_2008-02-11_2008-02-14"},
                         self._monitor._parse_watch_dict("77865342_ingest_AIRS_2008-02-11_2008-02-14"))

    def test_parse_watch_dict_three_jobs(self):
        cmd_line = "77865342_ingest_AIRS_2008-02-11_2008-02-14 77865343_ingest_AIRS_2008-02-15_2008-02-21 77865344_ingest_AIRS_2008-02-22_2008-02-27"

        watch_dict = self._monitor._parse_watch_dict(cmd_line)
        self.assertEqual(3, len(watch_dict))
        self.assertEqual("ingest_AIRS_2008-02-15_2008-02-21", watch_dict["77865343"])

    def test_parse_watch_dict_illegal_id_coding(self):
        cmd_line = "77865342-hu 77865344-ho 77865346-ha"

        try:
            self._monitor._parse_watch_dict(cmd_line)
            self.fail("ValueError expected")
        except ValueError:
            pass

    def test_resolve_jobs_empty(self):
        watch_dict = {}
        job_status_dict = {}

        resolved_dict, check_log_dict = self._monitor._resolve_jobs(watch_dict, job_status_dict)
        self.assertEqual(0, len(resolved_dict))
        self.assertEqual(0, len(check_log_dict))

    def test_resolve_jobs_empty_watch_list(self):
        watch_dict = {}
        job_status_dict = {"123456": StatusCodes.SCHEDULED, "123457": StatusCodes.RUNNING}

        resolved_dict, check_log_dict = self._monitor._resolve_jobs(watch_dict, job_status_dict)
        self.assertEqual(0, len(resolved_dict))
        self.assertEqual(0, len(check_log_dict))

    def test_resolve_jobs_only_check_log(self):
        watch_dict = {"77865343": "ingest_AIRS_2008-02-15_2008-02-21", "77865345": "ingest_AIRS_2008-02-22_2008-02-27",
                      "77865346": "ingest_AIRS_2008-02-28_2008-03-02"}
        job_status_dict = {}

        resolved_dict, check_log_dict = self._monitor._resolve_jobs(watch_dict, job_status_dict)
        self.assertEqual(0, len(resolved_dict))
        self.assertEqual(3, len(check_log_dict))
        self.assertEqual("ingest_AIRS_2008-02-22_2008-02-27", check_log_dict["77865345"])

    def test_resolve_jobs_only_resolved(self):
        watch_dict = {"77865347": "ingest_AIRS_2008-02-15_2008-02-21", "77865348": "ingest_AIRS_2008-02-22_2008-02-27",
                      "77865349": "ingest_AIRS_2008-02-28_2008-03-02"}
        job_status_dict = {"77865348": StatusCodes.SCHEDULED, "77865347": StatusCodes.RUNNING,
                           "77865349": StatusCodes.RUNNING}

        resolved_dict, check_log_dict = self._monitor._resolve_jobs(watch_dict, job_status_dict)
        self.assertEqual(3, len(resolved_dict))
        self.assertEqual(0, len(check_log_dict))

        values = resolved_dict["77865348"]
        self.assertEqual(3, len(values))
        self.assertEqual("ingest_AIRS_2008-02-22_2008-02-27", values[0])
        self.assertEqual(StatusCodes.SCHEDULED, values[1])
        self.assertEqual("", values[2])

    def test_resolve_jobs_both_possibilities(self):
        watch_dict = {"77865350": "ingest_AIRS_2008-02-15_2008-02-21", "77865351": "ingest_AIRS_2008-02-22_2008-02-27",
                      "77865352": "ingest_AIRS_2008-02-28_2008-03-02", "77865353": "ingest_AIRS_2008-03-03_2008-03-08",
                      "77865354": "ingest_AIRS_2008-03-09_2008-03-15", "77865355": "ingest_AIRS_2008-03-16_2008-03-22"}
        job_status_dict = {"77865355": StatusCodes.SCHEDULED, "77865352": StatusCodes.RUNNING,
                           "77865351": StatusCodes.RUNNING, "77865354": StatusCodes.UNKNOWN}

        resolved_dict, check_log_dict = self._monitor._resolve_jobs(watch_dict, job_status_dict)
        self.assertEqual(4, len(resolved_dict))
        self.assertEqual(2, len(check_log_dict))

        values = resolved_dict["77865354"]
        self.assertEqual(3, len(values))
        self.assertEqual("ingest_AIRS_2008-03-09_2008-03-15", values[0])
        self.assertEqual(StatusCodes.UNKNOWN, values[1])
        self.assertEqual("", values[2])

        self.assertEqual("ingest_AIRS_2008-02-15_2008-02-21", check_log_dict["77865350"])
        self.assertEqual("ingest_AIRS_2008-03-03_2008-03-08", check_log_dict["77865353"])

    def test_get_log_dir_set(self):
        res_dir = self.get_test_res_dir()
        os.environ["PM_LOG_DIR"] = res_dir

        try:
            self.assertEqual(res_dir, self._monitor._get_log_dir())
        finally:
            os.environ.pop("PM_LOG_DIR", None)

    def test_get_log_dir_set_to_invalid(self):
        os.environ["PM_LOG_DIR"] = "/some/weired/system/path"

        try:
            self._monitor._get_log_dir()
            self.fail("ValueError expected")
        except ValueError:
            pass
        finally:
            os.environ.pop("PM_LOG_DIR", None)

    def test_get_log_dir_missing(self):
        try:
            self._monitor._get_log_dir()
            self.fail("RuntimeError expected")
        except RuntimeError:
            pass

    def test_resolve_status_from_log_empty(self):
        res_dir = self.get_test_res_dir()
        os.environ["PM_LOG_DIR"] = res_dir

        try:
            check_log = {}

            resolved = self._monitor._resolve_status_from_log(check_log)

            self.assertEqual(0, len(resolved))
        finally:
            os.environ.pop("PM_LOG_DIR", None)

    def test_resolve_status_from_log_success_one_job(self):
        res_dir = self.get_test_res_dir()
        os.environ["PM_LOG_DIR"] = res_dir

        try:
            check_log = {"887766554": "successfully_processed"}

            resolved = self._monitor._resolve_status_from_log(check_log)

            self.assertEqual(1, len(resolved))
            values = resolved["887766554"]
            self.assertEqual("successfully_processed", values[0])
            self.assertEqual(StatusCodes.DONE, values[1])
            self.assertEqual("", values[2])

        finally:
            os.environ.pop("PM_LOG_DIR", None)

    def test_resolve_status_from_log_success_three_job(self):
        res_dir = self.get_test_res_dir()
        os.environ["PM_LOG_DIR"] = res_dir

        try:
            check_log = {"887766554": "successfully_processed", "887766555": "successfully_processed_1", "887766556": "successfully_processed_2"}

            resolved = self._monitor._resolve_status_from_log(check_log)

            self.assertEqual(3, len(resolved))
            values = resolved["887766554"]
            self.assertEqual("successfully_processed", values[0])
            self.assertEqual(StatusCodes.DONE, values[1])
            self.assertEqual("", values[2])

            values = resolved["887766556"]
            self.assertEqual("successfully_processed_2", values[0])
            self.assertEqual(StatusCodes.DONE, values[1])
            self.assertEqual("", values[2])

        finally:
            os.environ.pop("PM_LOG_DIR", None)

    def test_resolve_status_from_log_failed(self):
        res_dir = self.get_test_res_dir()
        os.environ["PM_LOG_DIR"] = res_dir

        try:
            check_log = {"887766554": "processing_failed"}

            resolved = self._monitor._resolve_status_from_log(check_log)

            self.assertEqual(1, len(resolved))
            values = resolved["887766554"]
            self.assertEqual("processing_failed", values[0])
            self.assertEqual(StatusCodes.FAILED, values[1])
            self.assertTrue("processing_failed" in values[2])

        finally:
            os.environ.pop("PM_LOG_DIR", None)

    def test_format_and_write_empty(self):
        results = {}
        stream = StringIO()

        self._monitor._format_and_write(results, stream)
        self.assertEqual("", stream.getvalue())

    def test_format_and_write_one_result(self):
        results = {"8742536": ["ingest_AIRS_2008-02-22_2008-02-27", StatusCodes.DONE, ""]}
        stream = StringIO()

        self._monitor._format_and_write(results, stream)
        self.assertEqual("8742536_ingest_AIRS_2008-02-22_2008-02-27,DONE,\n", stream.getvalue())

    def test_format_and_write_three_results(self):
        results = {"8742536": ["ingest_AIRS_2008-02-22_2008-02-27", StatusCodes.DONE, ""],
                   "8742537": ["ingest_AIRS_2008-02-28_2008-03-02", StatusCodes.SCHEDULED, ""],
                   "8742538": ["ingest_AIRS_2008-03-03_2008-03-09", StatusCodes.FAILED, "sorry das ging schief: BÄNG"]}
        stream = StringIO()

        self._monitor._format_and_write(results, stream)
        self.assertEqual(
            "8742536_ingest_AIRS_2008-02-22_2008-02-27,DONE,\n8742537_ingest_AIRS_2008-02-28_2008-03-02,SCHEDULED,\n8742538_ingest_AIRS_2008-03-03_2008-03-09,FAILED,sorry das ging schief: BÄNG\n",
            stream.getvalue())

    @staticmethod
    def get_test_res_dir():
        return os.path.normpath(os.path.join(os.path.dirname(__file__), 'res'))
