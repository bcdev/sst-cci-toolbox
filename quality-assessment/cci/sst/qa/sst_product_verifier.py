import os
import re

import numpy as np
import numpy.ma as ma
from netCDF4._netCDF4 import Dataset

from cci.sst.qa.product_type import L2P, L3U, L4
from cci.sst.qa.verification_error import VerificationError


class SstProductVerifier:

    def __init__(self, report, source_pathname):
        self.report = report
        self.source_pathname = source_pathname
        self.filename_patterns = {
            '[0-9]{14}-ESACCI-L2P_GHRSST-.*\\.nc': L2P(),
            '[0-9]{14}-ESACCI-L3U_GHRSST-.*\\.nc': L3U(),
            '[0-9]{14}-ESACCI-L4_GHRSST-.*\\.nc': L4(),
        }

    def check_source_pathname(self):
        ok = os.path.isfile(self.source_pathname)
        if ok:
            self.report['source_pathname_check'] = float(0)
        else:
            self.report['source_pathname_check'] = float(1)
            self.report['source_pathname_check_failed_for'] = self.source_pathname
            raise VerificationError

    def check_source_filename(self):
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

    def check_product_can_be_opened(self):
        """

        :rtype : Dataset
        """
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

    def check_product_version(self, dataset):
        """

        :rtype : str
        """
        try:
            version = dataset.getncattr("product_version")
            if version is None:
                raise VerificationError

            self.report['product_has_version_check'] = float(0)
            return version
        except:
            self.report['product_has_version_check'] = float(1)
            filename = os.path.basename(self.source_pathname)
            self.report['product_has_version_failed_for'] = filename
            raise VerificationError

    def check_dataset(self, dataset, product_type):
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

    def _check_variable_limits(self, dataset):
        """

        :type dataset: Dataset
        """
        for variable_name in dataset.variables:
            variable = dataset.variables[variable_name]
            self.report[variable_name + '.count.total'] = float(variable.size)

            data = self.__get_masked_data(variable)
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
        if len(spec) == 4:
            a = SstProductVerifier.__get_data(dataset, spec[0], scale=True)
            b = SstProductVerifier.__get_data(dataset, spec[1], scale=True)
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
                    a = SstProductVerifier.__get_data_of_quality(dataset, reference_variable_name, quality_data, l).mask
                    b = SstProductVerifier.__get_data_of_quality(dataset, objective_variable_name, quality_data, l).mask
                    # false negatives: element is not masked in a, but masked in b
                    check_name = objective_variable_name + '.' + 'mask_false_negative_check_' + str(l)
                    self.__check_false_negatives(a, b, check_name)
                    # false positives: element is masked in a, but not masked in b
                    check_name = objective_variable_name + '.' + 'mask_false_positive_check_' + str(l)
                    self.__check_false_positives(a, b, check_name)
            else:
                a = SstProductVerifier.__get_data(dataset, reference_variable_name).mask
                b = SstProductVerifier.__get_data(dataset, objective_variable_name).mask
                # false negatives: element is not masked in a, but masked in b
                check_name = objective_variable_name + '.mask_false_negative_check'
                self.__check_false_negatives(a, b, check_name)
                # false positives: element is masked in a, but not masked in b
                check_name = objective_variable_name + '.' + 'mask_false_positive_check'
                self.__check_false_positives(a, b, check_name)

    def _check_corruptness(self, dataset, product_type):
        """

        :type dataset: Dataset
        :type product_type: ProductType
        """
        ok = True
        for variable_name in product_type.get_sst_variable_names():
            if variable_name in dataset.variables:
                variable = dataset.variables[variable_name]

                data = SstProductVerifier.__get_masked_data(variable)
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
    def __get_data(dataset, variable_name, scale=False):
        """

        :type dataset: Dataset
        :type variable_name: str
        :type scale: bool
        :rtype : ma.MaskedArray
        """
        variable = dataset.variables[variable_name]
        data = SstProductVerifier.__get_masked_data(variable)
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
        data = SstProductVerifier.__get_masked_data_of_quality(variable, quality_data, quality_level)
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
