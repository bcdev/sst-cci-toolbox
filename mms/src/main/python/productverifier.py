__author__ = 'ralf'

import os
import re
from netCDF4 import Dataset

import numpy
import numpy.ma as ma


class VerificationException(Exception):
    pass


class ProductType:
    def __init__(self, variable_names):
        """

        :type variable_names: list
        """
        self.variable_names = variable_names

    def get_variable_names(self):
        """

        :rtype : list
        """
        return self.variable_names


class L2P(ProductType):
    def __init__(self):
        ProductType.__init__(self, [
            'adjustment_uncertainty',
            'l2p_flags',
            'large_scale_correlated_uncertainty',
            'lat',
            'lon',
            'quality_level'
            'sea_surface_temperature',
            'sea_surface_temperature_depth',
            'sses_bias',
            'sses_standard_deviation',
            'sst_depth_total_uncertainty',
            'sst_dtime',
            'synoptically_correlated_uncertainty',
            'time',
            'uncorrelated_uncertainty',
            'wind_speed',
        ])

    def get_variable_names(self):
        """

        :rtype : list
        """
        return self.variable_names


class ProductVerifier:
    def __init__(self, source_pathname, report_pathname='report.properties'):
        """

        :type source_pathname: str
        :type report_pathname: str
        """
        self.source_pathname = source_pathname
        self.report_pathname = report_pathname
        self.report = {}
        self.filename_patterns = {'[0-9]{14}-ESACCI-L2P_GHRSST-.*\\.nc': L2P()}

    def get_source_pathname(self):
        """

        :rtype : str
        """
        return self.source_pathname

    def get_report_pathname(self):
        """

        :rtype : str
        """
        return self.report_pathname

    def get_report(self):
        return self.report

    def _check_source_pathname(self):
        ok = os.path.isfile(self.source_pathname)
        if ok:
            self.report['source_pathname_check'] = 0
        else:
            self.report['source_pathname_check'] = 1
            raise VerificationException

    def _check_source_filename(self):
        """

        :rtype : ProductType
        """
        product_type = None

        filename = os.path.basename(self.source_pathname)
        for p, t in self.filename_patterns.iteritems():
            if re.match(p, filename):
                product_type = t
                break

        if product_type is not None:
            self.report['source_filename_check'] = 0
            return product_type
        else:
            self.report['source_filename_check'] = 1
            raise VerificationException

    def _check_variable_existence(self, dataset, product_type):
        """

        :type dataset: Dataset
        :type product_type: ProductType
        """
        for variable_name in product_type.get_variable_names():
            try:
                # noinspection PyUnusedLocal
                variable = dataset.variables[variable_name]
                self.report[variable_name + '.exists'] = 0
            except KeyError:
                self.report[variable_name + '.exists'] = 1

    @staticmethod
    def __get_masked_data(variable):
        """

        :rtype : ma.MaskedArray
        """
        data = variable[:]
        try:
            fill_value = variable.getncattr('_FillValue')
            return ma.masked_equal(data, fill_value)
        except AttributeError:
            return ma.array(data)

    @staticmethod
    def __get_data(dataset, variable_name):
        """

        :type dataset: Dataset
        :type variable_name: str

        :rtype : ma.MaskedArray
        """
        return ProductVerifier.__get_masked_data(dataset.variables[variable_name])

    def _check_variable_limits(self, dataset):
        """

        :type dataset: Dataset
        """
        for variable_name in dataset.variables:
            variable = dataset.variables[variable_name]
            self.report[variable_name + '.count.total'] = variable.size

            data = ProductVerifier.__get_masked_data(variable)
            self.report[variable_name + '.count.valid'] = data.count()

            try:
                valid_max = variable.getncattr('valid_max')
                invalid_data = ma.masked_less_or_equal(data, valid_max)
                self.report[variable_name + '.count.valid_max_failure'] = invalid_data.count()
            except AttributeError:
                pass
            try:
                valid_min = variable.getncattr('valid_min')
                invalid_data = ma.masked_greater_or_equal(data, valid_min)
                self.report[variable_name + '.count.valid_min_failure'] = invalid_data.count()
            except AttributeError:
                pass
            try:
                valid_max = variable.getncattr('valid_max')
                valid_min = variable.getncattr('valid_min')
                valid_data = ma.masked_outside(data, valid_min, valid_max)
                self.report[variable_name + '.count.inside'] = valid_data.count()
            except AttributeError:
                pass

    def _check_geophysical(self, dataset):
        """

        :type dataset: Dataset
        """
        sst_data = ProductVerifier.__get_data(dataset, 'sea_surface_temperature')
        sst_depth_data = ProductVerifier.__get_data(dataset, 'sea_surface_temperature_depth')
        sst_difference_data = sst_data - sst_depth_data
        suspicious_data = ma.masked_inside(sst_difference_data, -5.0, 10.0)
        self.report['geophysical_check'] = suspicious_data.count()

    def _check_mask_consistency(self, dataset, reference_variable_name, objective_variable_name):
        """

        :type dataset: Dataset
        :type reference_variable_name: str
        :type objective_variable_name: str
        """
        # noinspection PyNoneFunctionAssignment
        a = ma.getmaskarray(ProductVerifier.__get_data(dataset, reference_variable_name))
        # noinspection PyNoneFunctionAssignment
        b = ma.getmaskarray(ProductVerifier.__get_data(dataset, objective_variable_name))
        false_negatives = ma.masked_equal(numpy.logical_or(numpy.logical_not(a), b), True)
        self.report[objective_variable_name + '.mask.failure.false_negative'] = false_negatives.count()
        false_positives = ma.masked_equal(numpy.logical_or(numpy.logical_not(b), a), True)
        self.report[objective_variable_name + '.mask.failure.false_positive'] = false_positives.count()

    def _check_dataset(self, product_type):
        """

        :type product_type: ProductType
        """
        try:
            dataset = Dataset(self.source_pathname)
        except RuntimeError:
            raise VerificationException
        try:
            self._check_variable_existence(dataset, product_type)
            self._check_variable_limits(dataset)
            self._check_geophysical(dataset)
            self._check_mask_consistency(dataset, 'sea_surface_temperature', 'sses_bias')
            self._check_mask_consistency(dataset, 'sea_surface_temperature', 'sses_standard_deviation')
            self._check_mask_consistency(dataset, 'sea_surface_temperature', 'adjustment_uncertainty')
            self._check_mask_consistency(dataset, 'sea_surface_temperature', 'large_scale_correlated_uncertainty')
            self._check_mask_consistency(dataset, 'sea_surface_temperature', 'synoptically_correlated_uncertainty')
            self._check_mask_consistency(dataset, 'sea_surface_temperature', 'uncorrelated_uncertainty')
            self._check_mask_consistency(dataset, 'sea_surface_temperature_depth', 'sst_depth_total_uncertainty')
        except:
            raise VerificationException
        finally:
            dataset.close()

    def write_report(self):
        report_file = open(self.report_pathname, 'w')
        try:
            report_keys = sorted(self.report.keys())
            for key in report_keys:
                report_file.write(key + ' = ' + str(self.report[key]) + '\n')
        finally:
            report_file.close()

    def verify(self):
        self._check_source_pathname()
        product_type = self._check_source_filename()
        self._check_dataset(product_type)
        self.write_report()


if __name__ == "__main__":
    # Call with two arguments:
    #
    # 1 = source filepath
    # 2 = report filepath
    import sys

    verifier = ProductVerifier(sys.argv[1], sys.argv[2])
    # noinspection PyBroadException
    try:
        verifier.verify()
        sys.exit(0)
    except VerificationException:
        sys.exit(1)
    except Exception:
        sys.exit(2)
