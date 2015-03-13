__author__ = 'ralf'

import os
import re
from netCDF4 import Dataset
import json

import numpy
import numpy.ma as ma


class VerificationException(Exception):
    pass


class ProductType:
    def __init__(self, variable_names, geophysical_check_spec, mask_consistency_check_specs):
        """

        :type variable_names: list
        :type geophysical_check_spec: list
        :type mask_consistency_check_specs: list
        """
        self.variable_names = variable_names
        self.geophysical_check_spec = geophysical_check_spec
        self.mask_consistency_check_specs = mask_consistency_check_specs

    def get_variable_names(self):
        """

        :rtype : list
        """
        return self.variable_names

    def get_geophysical_check_spec(self):
        """

        :rtype : list
        """
        return self.geophysical_check_spec

    def get_mask_consistency_check_specs(self):
        """

        :rtype : list
        """
        return self.mask_consistency_check_specs


class L2P(ProductType):
    def __init__(self):
        ProductType.__init__(self,
                             [
                                 'lat',
                                 'lon',
                                 'time',
                                 'sst_dtime',
                                 'sea_surface_temperature',
                                 'sea_surface_temperature_depth',
                                 'wind_speed',
                                 'quality_level',
                                 'sses_standard_deviation',
                                 'sst_depth_total_uncertainty',
                                 'large_scale_correlated_uncertainty',
                                 'synoptically_correlated_uncertainty',
                                 'uncorrelated_uncertainty',
                                 'adjustment_uncertainty',
                                 'l2p_flags',
                                 'sses_bias'
                             ],
                             ['sea_surface_temperature', 'sea_surface_temperature_depth', -5.0, 10.0],
                             [
                                 ['sea_surface_temperature', 'sses_bias'],
                                 ['sea_surface_temperature', 'sses_standard_deviation'],
                                 ['sea_surface_temperature', 'large_scale_correlated_uncertainty'],
                                 ['sea_surface_temperature', 'synoptically_correlated_uncertainty'],
                                 ['sea_surface_temperature', 'uncorrelated_uncertainty'],
                                 ['sea_surface_temperature', 'adjustment_uncertainty'],
                                 ['sea_surface_temperature_depth', 'sst_depth_total_uncertainty']
                             ])


class L3U(ProductType):
    def __init__(self):
        ProductType.__init__(self,
                             [
                                 'lat',
                                 'lat_bnds',
                                 'lon',
                                 'lon_bnds',
                                 'time',
                                 'time_bnds',
                                 'sst_dtime',
                                 'sea_surface_temperature',
                                 'sea_surface_temperature_depth',
                                 'wind_speed',
                                 'quality_level',
                                 'sses_standard_deviation',
                                 'sst_depth_total_uncertainty',
                                 'large_scale_correlated_uncertainty',
                                 'synoptically_correlated_uncertainty',
                                 'uncorrelated_uncertainty',
                                 'adjustment_uncertainty',
                                 'l2p_flags',
                                 'sses_bias'
                             ],
                             ['sea_surface_temperature', 'sea_surface_temperature_depth', -5.0, 10.0],
                             [
                                 ['sea_surface_temperature', 'sses_bias'],
                                 ['sea_surface_temperature', 'sses_standard_deviation'],
                                 ['sea_surface_temperature', 'large_scale_correlated_uncertainty'],
                                 ['sea_surface_temperature', 'synoptically_correlated_uncertainty'],
                                 ['sea_surface_temperature', 'uncorrelated_uncertainty'],
                                 ['sea_surface_temperature', 'adjustment_uncertainty'],
                                 ['sea_surface_temperature_depth', 'sst_depth_total_uncertainty']
                             ])


class L4(ProductType):
    def __init__(self):
        ProductType.__init__(self,
                             [
                                 'lat',
                                 'lat_bnds',
                                 'lon',
                                 'lon_bnds',
                                 'time',
                                 'time_bnds',
                                 'analysed_sst',
                                 'sea_ice_fraction',
                                 'quality_level'
                                 'analysis_error',
                                 'sea_ice_fraction_error',
                                 'mask',
                             ], [],
                             [
                                 ['analysed_sst', 'analysis_error'],
                                 ['sea_ice_fraction', 'sea_ice_fraction_error']
                             ])


class ProductVerifier:
    def __init__(self, source_pathname, report_pathname=None):
        """

        :type source_pathname: str
        :type report_pathname: str
        """
        self.source_pathname = source_pathname
        self.report_pathname = report_pathname
        self.report = {}
        self.filename_patterns = {
            '[0-9]{14}-ESACCI-L2P_GHRSST-.*\\.nc': L2P(),
        }

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

    def verify(self):
        self._check_source_pathname()
        product_type = self._check_source_filename()
        self._check_dataset(product_type)
        if self.report_pathname is not None:
            self._write_report()
        else:
            print json.dumps(self.report)

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
                self.report[variable_name + '.existence_check'] = 0
            except KeyError:
                self.report[variable_name + '.existence_check'] = 1

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
                invalid_data = ma.masked_less_equal(data, valid_max)
                self.report[variable_name + '.valid_max_check'] = invalid_data.count()
            except AttributeError:
                pass
            try:
                valid_min = variable.getncattr('valid_min')
                invalid_data = ma.masked_greater_equal(data, valid_min)
                self.report[variable_name + '.valid_min_check'] = invalid_data.count()
            except AttributeError:
                pass

    def _check_geophysical(self, dataset, product_type):
        """

        :type dataset: Dataset
        :type product_type: ProductType
        """
        spec = product_type.get_geophysical_check_spec()
        if len(spec) != 0:
            a = ProductVerifier.__get_data(dataset, spec[0])
            b = ProductVerifier.__get_data(dataset, spec[1])
            difference_data = a - b
            suspicious_data = ma.masked_inside(difference_data, spec[2], spec[3])
            self.report['geophysical_check'] = suspicious_data.count()

    # noinspection PyNoneFunctionAssignment,PyUnresolvedReferences
    def _check_mask_consistency(self, dataset, product_type):
        """

        :type dataset: Dataset
        :type product_type: ProductType
        """
        for spec in product_type.get_mask_consistency_check_specs():
            reference_variable_name = spec[0]
            objective_variable_name = spec[1]
            a = ma.getmaskarray(ProductVerifier.__get_data(dataset, reference_variable_name))
            b = ma.getmaskarray(ProductVerifier.__get_data(dataset, objective_variable_name))
            # false negatives: element is not masked in a, but masked in b
            false_negatives = ma.masked_equal(numpy.logical_or(numpy.logical_not(a), b), True)
            self.report[objective_variable_name + '.mask_false_negative_check'] = false_negatives.count()
            # false positives: element is masked in a, but not masked in b
            false_positives = ma.masked_equal(numpy.logical_or(numpy.logical_not(b), a), True)
            self.report[objective_variable_name + '.mask_false_positive_check'] = false_positives.count()

    def _check_dataset(self, product_type):
        """

        :type product_type: ProductType
        """
        try:
            dataset = Dataset(self.source_pathname)
        except:
            raise VerificationException
        try:
            self._check_variable_existence(dataset, product_type)
            self._check_variable_limits(dataset)
            self._check_geophysical(dataset, product_type)
            self._check_mask_consistency(dataset, product_type)

        except:
            raise VerificationException
        finally:
            dataset.close()


    @staticmethod
    def load_report(report_pathname):
        """

        :type report_pathname: str
        :rtype : dict
        """
        report_file = open(report_pathname, 'r')
        try:
            return json.load(report_file)
        finally:
            report_file.close()

    @staticmethod
    def dump_report(report, report_pathname):
        """

        :type report: dict
        :type report_pathname: str
        """
        report_file = open(report_pathname, 'w')
        try:
            json.dump(report, report_file, indent=1)
        finally:
            report_file.close()

    def _write_report(self):
        ProductVerifier.dump_report(self.report, self.report_pathname)


if __name__ == "__main__":
    # Call with two arguments:
    #
    # 1 = source pathname
    # 2 = report pathname
    import sys

    verifier = ProductVerifier(sys.argv[1], sys.argv[2])
    # noinspection PyBroadException
    try:
        verifier.verify()
        sys.exit(0)
    except VerificationException:
        sys.exit(1)
    except:
        sys.exit(2)
