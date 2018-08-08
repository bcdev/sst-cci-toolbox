__author__ = 'Ralf Quast'

import os
import re
from netCDF4 import Dataset
import json
from time import gmtime, strftime

import numpy as np
import numpy.ma as ma


class VerificationError(Exception):
    exit_code = 2


class ProductType:
    def __init__(self, variable_names, geophysical_check_spec, mask_consistency_check_specs, sst_variable_names):
        """

        :type variable_names: list
        :type geophysical_check_spec: list
        :type mask_consistency_check_specs: list
        :type sst_variable_names: list
        """
        self.variable_names = variable_names
        self.geophysical_check_spec = geophysical_check_spec
        self.mask_consistency_check_specs = mask_consistency_check_specs
        self.sst_variable_names = sst_variable_names

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

    def get_sst_variable_names(self):
        """

        :rtype : list
        """
        return self.sst_variable_names


class L2P(ProductType):
    def __init__(self):
        ProductType.__init__(self,
                             [
                                 'adjustment_uncertainty',
                                 'l2p_flags',
                                 'large_scale_correlated_uncertainty',
                                 'lat',
                                 'lon',
                                 'quality_level',
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
                             ],
                             ['sea_surface_temperature', 'sea_surface_temperature_depth', -5.0, 10.0],
                             [
                                 ['sea_surface_temperature', 'quality_level', 'quality_level',
                                  [0, 2, 3, 4, 5]],
                                 ['sea_surface_temperature', 'sses_bias', 'quality_level',
                                  range(0, 6)],
                                 ['sea_surface_temperature', 'sses_standard_deviation', 'quality_level',
                                  range(0, 6)],
                                 ['sea_surface_temperature', 'large_scale_correlated_uncertainty', 'quality_level',
                                  range(0, 6)],
                                 ['sea_surface_temperature', 'synoptically_correlated_uncertainty', 'quality_level',
                                  range(0, 6)],
                                 ['sea_surface_temperature', 'uncorrelated_uncertainty', 'quality_level',
                                  range(0, 6)],
                                 ['sea_surface_temperature', 'adjustment_uncertainty', 'quality_level',
                                  range(0, 6)],
                                 ['sea_surface_temperature', 'sea_surface_temperature_depth', 'quality_level',
                                  range(0, 6)],
                                 ['sea_surface_temperature_depth', 'sst_depth_total_uncertainty', 'quality_level',
                                  range(0, 6)],
                             ],
                             [
                                 'sea_surface_temperature',
                                 'sea_surface_temperature_depth'
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
                                 'sses_bias',
                                 'aerosol_dynamic_indicator',
                                 'sensitivity'
                             ],
                             ['sea_surface_temperature', 'sea_surface_temperature_depth', -5.0, 10.0],
                             [
                                 ['sea_surface_temperature', 'quality_level', 'quality_level',
                                  [0, 2, 3, 4, 5]],
                                 ['sea_surface_temperature', 'sses_bias', 'quality_level',
                                  range(0, 6)],
                                 ['sea_surface_temperature', 'sses_standard_deviation', 'quality_level',
                                  range(0, 6)],
                                 ['sea_surface_temperature', 'large_scale_correlated_uncertainty', 'quality_level',
                                  range(0, 6)],
                                 ['sea_surface_temperature', 'synoptically_correlated_uncertainty', 'quality_level',
                                  range(0, 6)],
                                 ['sea_surface_temperature', 'uncorrelated_uncertainty', 'quality_level',
                                  range(0, 6)],
                                 ['sea_surface_temperature', 'adjustment_uncertainty', 'quality_level',
                                  range(0, 6)],
                                 ['sea_surface_temperature', 'sea_surface_temperature_depth', 'quality_level',
                                  range(0, 6)],
                                 ['sea_surface_temperature_depth', 'sst_depth_total_uncertainty', 'quality_level',
                                  range(0, 6)]
                             ],
                             [
                                 'sea_surface_temperature',
                                 'sea_surface_temperature_depth'
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
                                 ['analysed_sst', 'analysis_error', None],
                                 ['sea_ice_fraction', 'sea_ice_fraction_error', None]
                             ],
                             [
                                 'analysed_sst'
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
            '[0-9]{14}-ESACCI-L3U_GHRSST-.*\\.nc': L3U(),
            '[0-9]{14}-ESACCI-L4_GHRSST-.*\\.nc': L4(),
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
        try:
            print (ProductVerifier.get_current_time(), 'checking source pathname')
            self._check_source_pathname()
            print (ProductVerifier.get_current_time(), 'checking source filename')
            product_type = self._check_source_filename()
            print (ProductVerifier.get_current_time(), 'checking dataset can be opened')
            dataset = self._check_product_can_be_opened()
            print (ProductVerifier.get_current_time(), 'checking dataset')
            self._check_dataset(dataset, product_type)
        except VerificationError:
            print ('Verification error:', self.source_pathname)
        finally:
            ProductVerifier.ensure_correct_integer_types(self.report)
            ProductVerifier.dump_report(self.report, self.report_pathname)

    def _check_source_pathname(self):
        ok = os.path.isfile(self.source_pathname)
        if ok:
            self.report['source_pathname_check'] = float(0)
        else:
            self.report['source_pathname_check'] = float(1)
            self.report['source_pathname_check_failed_for'] = self.source_pathname
            raise VerificationError

    def _check_source_filename(self):
        """

        :rtype : ProductType
        """
        product_type = None

        filename = os.path.basename(self.source_pathname)
        for p, t in self.filename_patterns.items():
            if re.match(p, filename):
                product_type = t
                break

        if product_type is not None:
            self.report['source_filename_check'] = float(0)
            return product_type
        else:
            self.report['source_filename_check'] = float(1)
            filename = os.path.basename(self.source_pathname)
            self.report['source_filename_check_failed_for'] = filename
            raise VerificationError

    def _check_variable_existence(self, dataset, product_type):
        """

        :type dataset: Dataset
        :type product_type: ProductType
        """
        for variable_name in product_type.get_variable_names():
            if variable_name in dataset.variables:
                self.report[variable_name + '.existence_check'] = float(0)
            else:
                self.report[variable_name + '.existence_check'] = float(1)
                filename = os.path.basename(self.source_pathname)
                self.report[variable_name + '.existence_check_failed_for'] = filename

    @staticmethod
    def __get_masked_data(variable):
        """

        :type variable: Variable
        :rtype : ma.MaskedArray
        """
        data = variable[:]
        try:
            fill_value = variable.getncattr('_FillValue')
            return ma.masked_equal(data, fill_value)
        except AttributeError:
            return ma.array(data)

    @staticmethod
    def __get_masked_data_of_quality(variable, quality_data, level):
        """

        :type variable: Variable
        :type quality_data: Object
        :type level: int
        :rtype : ma.MaskedArray
        """
        mask = ma.masked_not_equal(quality_data, level).mask
        data = ma.array(variable[:], mask=mask)
        try:
            fill_value = variable.getncattr('_FillValue')
            return ma.array(data, mask=ma.masked_equal(data, fill_value).mask)
        except AttributeError:
            return data

    @staticmethod
    def __get_data(dataset, variable_name, scale=False):
        """


        :type dataset: Dataset
        :type variable_name: str
        :type scale: bool
        :rtype : ma.MaskedArray
        """
        variable = dataset.variables[variable_name]
        data = ProductVerifier.__get_masked_data(variable)
        if scale:
            try:
                scale_factor = variable.getncattr('scale_factor')
                if scale_factor != 1.0:
                    data = data * scale_factor
            except AttributeError:
                pass
            try:
                add_offset = variable.getncattr('add_offset')
                if add_offset != 0.0:
                    data = data + add_offset
            except AttributeError:
                pass
        return data

    @staticmethod
    def __get_data_of_quality(dataset, variable_name, quality_data, quality_level, scale=False):
        """

        :type dataset: Dataset
        :type variable_name: str
        :type quality_data: Object
        :type quality_level: int
        :type scale: bool
        :rtype : ma.MaskedArray
        """
        variable = dataset.variables[variable_name]
        data = ProductVerifier.__get_masked_data_of_quality(variable, quality_data, quality_level)
        if scale:
            try:
                scale_factor = variable.getncattr('scale_factor')
                if scale_factor != 1.0:
                    data = data * scale_factor
            except AttributeError:
                pass
            try:
                add_offset = variable.getncattr('add_offset')
                if add_offset != 0.0:
                    data = data + add_offset
            except AttributeError:
                pass
        return data

    def _check_variable_limits(self, dataset):
        """

        :type dataset: Dataset
        """
        for variable_name in dataset.variables:
            variable = dataset.variables[variable_name]
            self.report[variable_name + '.count.total'] = float(variable.size)

            data = ProductVerifier.__get_masked_data(variable)
            self.report[variable_name + '.count.valid'] = float(data.count())

            try:
                valid_max = variable.getncattr('valid_max')
                invalid_data = ma.masked_less_equal(data, valid_max)
                invalid_data_count = invalid_data.count()
                if invalid_data_count == 0:
                    self.report[variable_name + '.valid_max_check'] = float(invalid_data_count)
                else:
                    variable.getncattr('_FillValue')
                    self.report[variable_name + '.valid_max_check'] = float(invalid_data_count)
                    filename = os.path.basename(self.source_pathname)
                    self.report[variable_name + '.valid_max_check_failed_for'] = filename
            except AttributeError:
                pass
            try:
                valid_min = variable.getncattr('valid_min')
                invalid_data = ma.masked_greater_equal(data, valid_min)
                invalid_data_count = invalid_data.count()
                self.report[variable_name + '.valid_min_check'] = float(invalid_data_count)
                if invalid_data_count == 0:
                    self.report[variable_name + '.valid_min_check'] = float(invalid_data_count)
                else:
                    variable.getncattr('_FillValue')
                    self.report[variable_name + '.valid_min_check'] = float(invalid_data_count)
                    filename = os.path.basename(self.source_pathname)
                    self.report[variable_name + '.valid_min_check_failed_for'] = filename
            except AttributeError:
                pass

    def _check_geophysical(self, dataset, product_type):
        """

        :type dataset: Dataset
        :type product_type: ProductType
        """
        spec = product_type.get_geophysical_check_spec()
        if len(spec) != 0:
            a = ProductVerifier.__get_data(dataset, spec[0], scale=True)
            b = ProductVerifier.__get_data(dataset, spec[1], scale=True)
            d = a - b
            # count pixels with differences less than the minimum
            suspicious_data = ma.masked_greater_equal(d, spec[2])
            suspicious_data_count = suspicious_data.count()
            self.report['geophysical_minimum_check'] = float(suspicious_data_count)
            if suspicious_data_count > 0:
                filename = os.path.basename(self.source_pathname)
                self.report['geophysical_minimum_check_failed_for'] = filename
            # count pixels with differences greater than the maximum
            suspicious_data = ma.masked_less_equal(d, spec[3])
            suspicious_data_count = suspicious_data.count()
            self.report['geophysical_maximum_check'] = float(suspicious_data_count)
            if suspicious_data_count > 0:
                filename = os.path.basename(self.source_pathname)
                self.report['geophysical_maximum_check_failed_for'] = filename

    def __check_false_negatives(self, reference_mask, objective_mask, check_name):
        # noinspection PyNoneFunctionAssignment,PyUnresolvedReferences
        false_negatives = ma.masked_equal(np.logical_or(np.logical_not(reference_mask), objective_mask), True)
        false_negatives_count = false_negatives.count()
        self.report[check_name] = float(false_negatives_count)
        if false_negatives_count > 0:
            filename = os.path.basename(self.source_pathname)
            self.report[check_name + '_failed_for'] = filename

    def __check_false_positives(self, reference_mask, objective_mask, check_name):
        # noinspection PyNoneFunctionAssignment,PyUnresolvedReferences
        false_positives = ma.masked_equal(np.logical_or(np.logical_not(objective_mask), reference_mask), True)
        false_positives_count = false_positives.count()
        self.report[check_name] = float(false_positives_count)
        if false_positives_count > 0:
            filename = os.path.basename(self.source_pathname)
            self.report[check_name + '_failed_for'] = filename

    def _check_mask_consistency(self, dataset, product_type):
        """

        :type dataset: Dataset
        :type product_type: ProductType
        """
        for spec in product_type.get_mask_consistency_check_specs():
            reference_variable_name = spec[0]
            objective_variable_name = spec[1]
            quality_variable_name = spec[2]

            if quality_variable_name in dataset.variables:
                quality_data = dataset.variables[quality_variable_name][:]
                quality_levels = spec[3]
                for l in quality_levels:
                    a = ProductVerifier.__get_data_of_quality(dataset, reference_variable_name, quality_data, l).mask
                    b = ProductVerifier.__get_data_of_quality(dataset, objective_variable_name, quality_data, l).mask
                    # false negatives: element is not masked in a, but masked in b
                    check_name = objective_variable_name + '.' + 'mask_false_negative_check_' + str(l)
                    self.__check_false_negatives(a, b, check_name)
                    # false positives: element is masked in a, but not masked in b
                    check_name = objective_variable_name + '.' + 'mask_false_positive_check_' + str(l)
                    self.__check_false_positives(a, b, check_name)
            else:
                a = ProductVerifier.__get_data(dataset, reference_variable_name).mask
                b = ProductVerifier.__get_data(dataset, objective_variable_name).mask
                # false negatives: element is not masked in a, but masked in b
                check_name = objective_variable_name + '.mask_false_negative_check'
                self.__check_false_negatives(a, b, check_name)
                # false positives: element is masked in a, but not masked in b
                check_name = objective_variable_name + '.' + 'mask_false_positive_check'
                self.__check_false_positives(a, b, check_name)

    def _check_product_can_be_opened(self):
        try:
            dataset = Dataset(self.source_pathname)
            dataset.set_auto_maskandscale(False)
            self.report['product_can_be_opened_check'] = float(0)
            return dataset
        except:
            self.report['product_can_be_opened_check'] = float(1)
            filename = os.path.basename(self.source_pathname)
            self.report['product_can_be_opened_check_failed_for'] = filename
            raise VerificationError

    def _check_dataset(self, dataset, product_type):
        """

        :type dataset: Dataset
        :type product_type: ProductType
        """
        try:
             self._check_variable_existence(dataset, product_type)
             self._check_variable_limits(dataset)
             self._check_geophysical(dataset, product_type)
             self._check_mask_consistency(dataset, product_type)
             self._check_corruptness(dataset, product_type)
        finally:
            try:
                dataset.close()
            except IOError:
                pass

    @staticmethod
    def ensure_correct_integer_types(report):
        """

        :type report: Dictionary
        """
        for key,value in report.items():
            if isinstance(value, np.int64):
                report[key] = value.astype(int)
            if isinstance(value, np.int32):
                report[key] = value.astype(int)


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
    def dump_report(report, report_pathname=None):
        """

        :type report: dict
        :type report_pathname: str
        """
        if report_pathname is None:
            print (json.dumps(report, indent=1, sort_keys=True))
        else:
            report_dirname = os.path.dirname(report_pathname)
            if not os.path.exists(report_dirname):
                os.makedirs(report_dirname)

            with open(report_pathname, 'w') as f:
                json.dump(report, f, indent=4, sort_keys=True)
            # report_file = open(report_pathname, 'w')
            # try:
            #     json.dump(report, report_file, indent=1, sort_keys=True)
            # finally:
            #     report_file.close()

    @staticmethod
    def get_current_time():
        return strftime("%Y-%m-%dT%H:%M:%S%Z", gmtime())

    def _check_corruptness(self, dataset, product_type):
        """

        :type dataset: Dataset
        :type product_type: ProductType
        """
        ok = True
        for variable_name in product_type.get_sst_variable_names():
            if variable_name in dataset.variables:
                variable = dataset.variables[variable_name]

                data = ProductVerifier.__get_masked_data(variable)
                valid_data_count = data.count()
                if valid_data_count == 0:
                    ok = False
                try:
                    valid_max = variable.getncattr('valid_max')
                    invalid_data = ma.masked_less_equal(data, valid_max)
                    valid_data_count = valid_data_count - invalid_data.count()
                except AttributeError:
                    pass
                try:
                    valid_min = variable.getncattr('valid_min')
                    invalid_data = ma.masked_greater_equal(data, valid_min)
                    valid_data_count = valid_data_count - invalid_data.count()
                except AttributeError:
                    pass
                if valid_data_count == 0:
                    ok = False
            else:
                ok = False
        if ok:
            self.report['corruptness_check'] = float(0)
        else:
            self.report['corruptness_check'] = float(1)
            filename = os.path.basename(self.source_pathname)
            self.report['corruptness_check_failed_for'] = filename
            raise VerificationError


def ensure_availability_of_imports():
    import sys

    # noinspection PyBroadException
    try:
        import netCDF4

        print (ProductVerifier.get_current_time(), 'The netCDF4 module is available')
    except:
        print (ProductVerifier.get_current_time(), 'The netCDF4 module is not available. Please install netCDF4.')
        sys.exit(1)


if __name__ == "__main__":
    # Call with one or two arguments:
    #
    # 1 = source pathname
    # 2 = report pathname (optional)
    import sys

    argument_count = len(sys.argv)
    if argument_count == 2:
        verifier = ProductVerifier(sys.argv[1])
    elif argument_count == 3:
        verifier = ProductVerifier(sys.argv[1], sys.argv[2])
    else:
        print ('usage:', sys.argv[0], '<source pathname> <report pathname>')
        sys.exit(1)

    ensure_availability_of_imports()

    # noinspection PyBroadException
    try:
        verifier.verify()
        sys.exit()
    except Exception as e:
        print ("Error {0}".format(e.args))
        sys.exit(1)
