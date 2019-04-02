__author__ = 'Ralf Quast'

import matplotlib

matplotlib.rc('axes', linewidth=1)
matplotlib.rc('font', size=9)
matplotlib.rc('legend', fontsize=9)
matplotlib.rc('lines', linewidth=1)
matplotlib.rc('patch', linewidth=0.25)
matplotlib.rc('patch', edgecolor='white')
matplotlib.use('PDF')

import matplotlib.patches as mpatches
import matplotlib.pyplot as plt
import numpy as np
import os

from cci.sst.qa.productverifier import ProductVerifier

FIG_1_CHECKS = {
    'Is File': ['source_pathname_check'],
    'Filename': ['source_filename_check'],
    'Can Open': ['product_can_be_opened_check'],
    'Has Version': ['product_has_version_check'],
    'Lat Exists': ['lat.existence_check'],
    'Lon Exists': ['lon.existence_check'],
    'SST Exists': ['sea_surface_temperature.existence_check'],
    'Time Exists': ['time.existence_check'],
    'SST DTime Exists': ['sst_dtime.existence_check'],
    'SSES Bias Exists': ['sses_bias.existence_check'],
    'SSES St Dev Exists': ['sses_standard_deviation.existence_check'],
    'Large Scale Unc Exists': ['large_scale_correlated_uncertainty.existence_check'],
    'Correlated Unc Exists': ['uncertainty_correlated.existence_check'],
    'Uncorrelated Unc Exists': ['uncorrelated_uncertainty.existence_check'],
    'Random Unc Exists': ['uncertainty_random.existence_check'],
    'Synoptic Unc Exists': ['synoptically_correlated_uncertainty.existence_check'],
    'Systematic Unc Exists': ['uncertainty_systematic.existence_check'],
    'SST Depth Exists': ['sea_surface_temperature_depth.existence_check'],
    'SST Depth Unc Exists': ['sst_depth_total_uncertainty.existence_check'],
    'SST Depth DTime Exists': ['sst_depth_dtime.existence_check'],
    'Wind Speed Exists': ['wind_speed.existence_check'],
    'L2P Flags Exist': ['l2p_flags.existence_check'],
    'Quality Level Exists': ['quality_level.existence_check'],
    'Adj Alt Exists': ['adjustment_alt.existence_check'],
    'Alt SST Retr Exists': ['alt_sst_retrieval_type.existence_check'],
    'Depth Adj Exists': ['depth_adjustment.existence_check'],
    'Emp Adj Exists': ['empirical_adjustment.existence_check'],
    'SST Depth Anom Exists': ['sea_surface_temperature_depth_anomaly.existence_check'],
    'SST Ret Type Exists': ['sea_surface_temperature_retrieval_type.existence_check'],
    'SST Total Unc Exists': ['sea_surface_temperature_total_uncertainty.existence_check'],
    'SST Sens Exists': ['sensitivity.existence_check'],
    'Unc Corr Alt Exists': ['uncertainty_correlated_alt.existence_check'],
    'Unc Corr TD Exists': ['uncertainty_correlated_time_and_depth_adjustment.existence_check'],
    'Unc Rand Alt Exists': ['uncertainty_random_alt.existence_check'],
    'Unc Sys Alt Exists': ['uncertainty_systematic_alt.existence_check'],
    'SST Corrupt': ['corruptness_check'],
    'Adjustment Unc Exists': ['adjustment_uncertainty.existence_check'],
    'Aerosol Dyn Ind Exists': ['aerosol_dynamic_indicator.existence_check'],
    'Probability Clear Exists': ['probability_clear.existence_check'],
    'Lat Bnds Exists': ['lat_bnds.existence_check'],
    'Lon Bnds Exists': ['lon_bnds.existence_check'],
    'Time Bnds Exists': ['time_bnds.existence_check'],
}

FIG_2_CHECKS_MIN_MAX = {
    'Lat Max': ['lat.valid_max_check'],
    'Lat Min': ['lat.valid_min_check'],
    'Lon Max': ['lon.valid_max_check'],
    'Lon Min': ['lon.valid_min_check'],
    'SST DTime Max': ['sst_dtime.valid_max_check'],
    'SST DTime Min': ['sst_dtime.valid_min_check'],
    'SST Max': ['sea_surface_temperature.valid_max_check'],
    'SST Min': ['sea_surface_temperature.valid_min_check'],
    'SST Geophysical Min': ['geophysical_minimum_check'],
    'SST Geophysical Max': ['geophysical_maximum_check'],
    'Quality Level Max': ['quality_level.valid_max_check'],
    'Quality Level Min': ['quality_level.valid_min_check'],
    'SSES Bias Max': ['sses_bias.valid_max_check'],
    'SSES Bias Min': ['sses_bias.valid_min_check'],
    'SSES St Dev Max': ['sses_standard_deviation.valid_max_check'],
    'SSES St Dev Min': ['sses_standard_deviation.valid_min_check'],
    'Correlated Unc Max': ['uncertainty_correlated.valid_max_check'],
    'Correlated Unc Min': ['uncertainty_correlated.valid_min_check'],
    'Random Unc Max': ['uncertainty_random.valid_max_check'],
    'Random Unc Min': ['uncertainty_random.valid_min_check'],
    'Systematic Unc Max': ['uncertainty_systematic.valid_max_check'],
    'Systematic Unc Min': ['uncertainty_systematic.valid_min_check'],
    'SST Depth Max': ['sea_surface_temperature_depth.valid_max_check'],
    'SST Depth Min': ['sea_surface_temperature_depth.valid_min_check'],
    'SST Depth Unc Max': ['sst_depth_total_uncertainty.valid_max_check', 'sea_surface_temperature_depth_total_uncertainty.valid_max_check'],
    'SST Depth Unc Min': ['sst_depth_total_uncertainty.valid_min_check', 'sea_surface_temperature_depth_total_uncertainty.valid_min_check'],
    'Wind Speed Max': ['wind_speed.valid_max_check'],
    'Wind Speed Min': ['wind_speed.valid_min_check'],
    'L2P Flags Max': ['l2p_flags.valid_max_check'],
    'L2P Flags Min': ['l2p_flags.valid_min_check'],
    'Alt Adj Max': ['adjustment_alt.valid_max_check'],
    'Alt Adj Min': ['adjustment_alt.valid_min_check'],
    'Sens Max': ['sst_sensitivity.valid_max_check'],
    'Sens Min': ['sst_sensitivity.valid_min_check'],
    'Alt SST Retr Max': ['alt_sst_retrieval_type.valid_max_check'],
    'Alt SST Retr Min': ['alt_sst_retrieval_type.valid_min_check'],
    'Depth Adj Max': ['depth_adjustment.valid_max_check'],
    'Depth Adj Min': ['depth_adjustment.valid_min_check'],
    'Emp Adj Max': ['empirical_adjustment.valid_max_check'],
    'Emp Adj Min': ['empirical_adjustment.valid_min_check'],
    'SST Depth Anom Max': ['sea_surface_temperature_depth_anomaly.valid_max_check'],
    'SST Depth Anom Min': ['sea_surface_temperature_depth_anomaly.valid_min_check'],
    'SST Depth DTime Max': ['sst_depth_dtime.valid_max_check'],
    'SST Depth DTime Min': ['sst_depth_dtime.valid_min_check'],
    'SST Ret Type Max': ['sea_surface_temperature_retrieval_type.valid_max_check'],
    'SST Ret Type Min': ['sea_surface_temperature_retrieval_type.valid_min_check'],
    'SST Total Unc Max': ['sea_surface_temperature_total_uncertainty.valid_max_check'],
    'SST Total Unc Min': ['sea_surface_temperature_total_uncertainty.valid_min_check'],
    'Unc Corr Alt Max': ['uncertainty_correlated_alt.valid_max_check'],
    'Unc Corr Alt Min': ['uncertainty_correlated_alt.valid_min_check'],
    'Unc Corr TD Max': ['uncertainty_correlated_time_and_depth_adjustment.valid_max_check'],
    'Unc Corr TD Min': ['uncertainty_correlated_time_and_depth_adjustment.valid_min_check'],
    'Unc Rand Alt Max': ['uncertainty_random_alt.valid_max_check'],
    'Unc Rand Alt Min': ['uncertainty_random_alt.valid_min_check'],
    'Unc Sys Alt Max': ['uncertainty_systematic_alt.valid_max_check'],
    'Unc Sys Alt Min': ['uncertainty_systematic_alt.valid_min_check'],
    'Adjustment Unc Max': ['adjustment_uncertainty.valid_max_check'],
    'Adjustment Unc Min': ['adjustment_uncertainty.valid_min_check'],
    'Large Scale Unc Max': ['large_scale_correlated_uncertainty.valid_max_check'],
    'Large Scale Unc Min': ['large_scale_correlated_uncertainty.valid_min_check'],
    'Synoptic Unc Max': ['synoptically_correlated_uncertainty.valid_max_check'],
    'Synoptic Unc Min': ['synoptically_correlated_uncertainty.valid_min_check'],
    'Uncorrelated Unc Max': ['uncorrelated_uncertainty.valid_max_check'],
    'Uncorrelated Unc Min': ['uncorrelated_uncertainty.valid_min_check']
}

FIG_2_CHECKS_MASK = {'Quality Level Mask N': ['quality_level.mask_false_negative_check_0',
                                              'quality_level.mask_false_negative_check_1',
                                              'quality_level.mask_false_negative_check_2',
                                              'quality_level.mask_false_negative_check_3',
                                              'quality_level.mask_false_negative_check_4',
                                              'quality_level.mask_false_negative_check_5'],
                     'Quality Level Mask P': ['quality_level.mask_false_positive_check_0',
                                              'quality_level.mask_false_positive_check_1',
                                              'quality_level.mask_false_positive_check_2',
                                              'quality_level.mask_false_positive_check_3',
                                              'quality_level.mask_false_positive_check_4',
                                              'quality_level.mask_false_positive_check_5'],
                     'SSES Bias Mask N': ['sses_bias.mask_false_negative_check_0',
                                          'sses_bias.mask_false_negative_check_1',
                                          'sses_bias.mask_false_negative_check_2',
                                          'sses_bias.mask_false_negative_check_3',
                                          'sses_bias.mask_false_negative_check_4',
                                          'sses_bias.mask_false_negative_check_5'],
                     'SSES Bias Mask P': ['sses_bias.mask_false_positive_check_0',
                                          'sses_bias.mask_false_positive_check_1',
                                          'sses_bias.mask_false_positive_check_2',
                                          'sses_bias.mask_false_positive_check_3',
                                          'sses_bias.mask_false_positive_check_4',
                                          'sses_bias.mask_false_positive_check_5'],
                     'SSES St Dev Mask N': ['sses_standard_deviation.mask_false_negative_check_0',
                                            'sses_standard_deviation.mask_false_negative_check_1',
                                            'sses_standard_deviation.mask_false_negative_check_2',
                                            'sses_standard_deviation.mask_false_negative_check_3',
                                            'sses_standard_deviation.mask_false_negative_check_4',
                                            'sses_standard_deviation.mask_false_negative_check_5'],
                     'SSES St Dev Mask P': ['sses_standard_deviation.mask_false_positive_check_0',
                                            'sses_standard_deviation.mask_false_positive_check_1',
                                            'sses_standard_deviation.mask_false_positive_check_2',
                                            'sses_standard_deviation.mask_false_positive_check_3',
                                            'sses_standard_deviation.mask_false_positive_check_4',
                                            'sses_standard_deviation.mask_false_positive_check_5'],
                     'Depth Adj Mask N': ['depth_adjustment.mask_false_negative_check_0',
                                          'depth_adjustment.mask_false_negative_check_1',
                                          'depth_adjustment.mask_false_negative_check_2',
                                          'depth_adjustment.mask_false_negative_check_3',
                                          'depth_adjustment.mask_false_negative_check_4',
                                          'depth_adjustment.mask_false_negative_check_5'],
                     'Depth Adj Mask P': ['depth_adjustment.mask_false_positive_check_0',
                                          'depth_adjustment.mask_false_positive_check_1',
                                          'depth_adjustment.mask_false_positive_check_2',
                                          'depth_adjustment.mask_false_positive_check_3',
                                          'depth_adjustment.mask_false_positive_check_4',
                                          'depth_adjustment.mask_false_positive_check_5'],
                     'SST Depth Anom Mask N': ['sea_surface_temperature_depth_anomaly.mask_false_negative_check_0',
                                               'sea_surface_temperature_depth_anomaly.mask_false_negative_check_1',
                                               'sea_surface_temperature_depth_anomaly.mask_false_negative_check_2',
                                               'sea_surface_temperature_depth_anomaly.mask_false_negative_check_3',
                                               'sea_surface_temperature_depth_anomaly.mask_false_negative_check_4',
                                               'sea_surface_temperature_depth_anomaly.mask_false_negative_check_5'],
                     'SST Depth Anom Mask P': ['depth_adjustment.mask_false_positive_check_0',
                                               'sea_surface_temperature_depth_anomaly.mask_false_positive_check_1',
                                               'sea_surface_temperature_depth_anomaly.mask_false_positive_check_2',
                                               'sea_surface_temperature_depth_anomaly.mask_false_positive_check_3',
                                               'sea_surface_temperature_depth_anomaly.mask_false_positive_check_4',
                                               'sea_surface_temperature_depth_anomaly.mask_false_positive_check_5'],
                     'SST Depth Unc Mask N': ['sst_depth_total_uncertainty.mask_false_negative_check_0',
                                              'sst_depth_total_uncertainty.mask_false_negative_check_1',
                                              'sst_depth_total_uncertainty.mask_false_negative_check_2',
                                              'sst_depth_total_uncertainty.mask_false_negative_check_3',
                                              'sst_depth_total_uncertainty.mask_false_negative_check_4',
                                              'sst_depth_total_uncertainty.mask_false_negative_check_5',
                                              'sea_surface_temperature_depth_total_uncertainty.mask_false_negative_check_0',
                                              'sea_surface_temperature_depth_total_uncertainty.mask_false_negative_check_1',
                                              'sea_surface_temperature_depth_total_uncertainty.mask_false_negative_check_2',
                                              'sea_surface_temperature_depth_total_uncertainty.mask_false_negative_check_3',
                                              'sea_surface_temperature_depth_total_uncertainty.mask_false_negative_check_4',
                                              'sea_surface_temperature_depth_total_uncertainty.mask_false_negative_check_5'],
                     'SST Depth Unc Mask P': ['sst_depth_total_uncertainty.mask_false_positive_check_0',
                                              'sst_depth_total_uncertainty.mask_false_positive_check_1',
                                              'sst_depth_total_uncertainty.mask_false_positive_check_2',
                                              'sst_depth_total_uncertainty.mask_false_positive_check_3',
                                              'sst_depth_total_uncertainty.mask_false_positive_check_4',
                                              'sst_depth_total_uncertainty.mask_false_positive_check_5',
                                              'sea_surface_temperature_depth_total_uncertainty.mask_false_positive_check_0',
                                              'sea_surface_temperature_depth_total_uncertainty.mask_false_positive_check_1',
                                              'sea_surface_temperature_depth_total_uncertainty.mask_false_positive_check_2',
                                              'sea_surface_temperature_depth_total_uncertainty.mask_false_positive_check_3',
                                              'sea_surface_temperature_depth_total_uncertainty.mask_false_positive_check_4',
                                              'sea_surface_temperature_depth_total_uncertainty.mask_false_positive_check_5'],
                     'SST Ret Type Mask N': ['sea_surface_temperature_retrieval_type.mask_false_negative_check_0',
                                             'sea_surface_temperature_retrieval_type.mask_false_negative_check_1',
                                             'sea_surface_temperature_retrieval_type.mask_false_negative_check_2',
                                             'sea_surface_temperature_retrieval_type.mask_false_negative_check_3',
                                             'sea_surface_temperature_retrieval_type.mask_false_negative_check_4',
                                             'sea_surface_temperature_retrieval_type.mask_false_negative_check_5'],
                     'SST Ret Type Mask P': ['sea_surface_temperature_retrieval_type.mask_false_positive_check_0',
                                             'sea_surface_temperature_retrieval_type.mask_false_positive_check_1',
                                             'sea_surface_temperature_retrieval_type.mask_false_positive_check_2',
                                             'sea_surface_temperature_retrieval_type.mask_false_positive_check_3',
                                             'sea_surface_temperature_retrieval_type.mask_false_positive_check_4',
                                             'sea_surface_temperature_retrieval_type.mask_false_positive_check_5'],
                     'SST Total Unc Mask N': ['sea_surface_temperature_total_uncertainty.mask_false_negative_check_0',
                                              'sea_surface_temperature_total_uncertainty.mask_false_negative_check_1',
                                              'sea_surface_temperature_total_uncertainty.mask_false_negative_check_2',
                                              'sea_surface_temperature_total_uncertainty.mask_false_negative_check_3',
                                              'sea_surface_temperature_total_uncertainty.mask_false_negative_check_4',
                                              'sea_surface_temperature_total_uncertainty.mask_false_negative_check_5'],
                     'SST Total Unc Mask P': ['sea_surface_temperature_total_uncertainty.mask_false_positive_check_0',
                                              'sea_surface_temperature_total_uncertainty.mask_false_positive_check_1',
                                              'sea_surface_temperature_total_uncertainty.mask_false_positive_check_2',
                                              'sea_surface_temperature_total_uncertainty.mask_false_positive_check_3',
                                              'sea_surface_temperature_total_uncertainty.mask_false_positive_check_4',
                                              'sea_surface_temperature_total_uncertainty.mask_false_positive_check_5'],
                     'SST Sens Mask N': ['sensitivity.mask_false_negative_check_0',
                                         'sensitivity.mask_false_negative_check_1',
                                         'sensitivity.mask_false_negative_check_2',
                                         'sensitivity.mask_false_negative_check_3',
                                         'sensitivity.mask_false_negative_check_4',
                                         'sensitivity.mask_false_negative_check_5'],
                     'SST Sens Mask P': ['sensitivity.mask_false_positive_check_0',
                                         'sensitivity.mask_false_positive_check_1',
                                         'sensitivity.mask_false_positive_check_2',
                                         'sensitivity.mask_false_positive_check_3',
                                         'sensitivity.mask_false_positive_check_4',
                                         'sensitivity.mask_false_positive_check_5'],
                     'Unc Corr Mask N': ['uncertainty_correlated.mask_false_negative_check_0',
                                         'uncertainty_correlated.mask_false_negative_check_1',
                                         'uncertainty_correlated.mask_false_negative_check_2',
                                         'uncertainty_correlated.mask_false_negative_check_3',
                                         'uncertainty_correlated.mask_false_negative_check_4',
                                         'uncertainty_correlated.mask_false_negative_check_5'],
                     'Unc Corr Mask P': ['uncertainty_correlated.mask_false_positive_check_0',
                                         'uncertainty_correlated.mask_false_positive_check_1',
                                         'uncertainty_correlated.mask_false_positive_check_2',
                                         'uncertainty_correlated.mask_false_positive_check_3',
                                         'uncertainty_correlated.mask_false_positive_check_4',
                                         'uncertainty_correlated.mask_false_positive_check_5'],
                     'SST Depth Mask N': ['sea_surface_temperature_depth.mask_false_negative_check_0',
                                          'sea_surface_temperature_depth.mask_false_negative_check_1',
                                          'sea_surface_temperature_depth.mask_false_negative_check_2',
                                          'sea_surface_temperature_depth.mask_false_negative_check_3',
                                          'sea_surface_temperature_depth.mask_false_negative_check_4',
                                          'sea_surface_temperature_depth.mask_false_negative_check_5'],
                     'SST Depth Mask P': ['sea_surface_temperature_depth.mask_false_positive_check_0',
                                          'sea_surface_temperature_depth.mask_false_positive_check_1',
                                          'sea_surface_temperature_depth.mask_false_positive_check_2',
                                          'sea_surface_temperature_depth.mask_false_positive_check_3',
                                          'sea_surface_temperature_depth.mask_false_positive_check_4',
                                          'sea_surface_temperature_depth.mask_false_positive_check_5'],
                     'Unc Corr Alt Mask N': ['uncertainty_correlated_alt.mask_false_negative_check_0',
                                             'uncertainty_correlated_alt.mask_false_negative_check_1',
                                             'uncertainty_correlated_alt.mask_false_negative_check_2',
                                             'uncertainty_correlated_alt.mask_false_negative_check_3',
                                             'uncertainty_correlated_alt.mask_false_negative_check_4',
                                             'uncertainty_correlated_alt.mask_false_negative_check_5'],
                     'Unc Corr Alt Mask P': ['uncertainty_correlated_alt.mask_false_positive_check_0',
                                             'uncertainty_correlated_alt.mask_false_positive_check_1',
                                             'uncertainty_correlated_alt.mask_false_positive_check_2',
                                             'uncertainty_correlated_alt.mask_false_positive_check_3',
                                             'uncertainty_correlated_alt.mask_false_positive_check_4',
                                             'uncertainty_correlated_alt.mask_false_positive_check_5'],
                     'Unc Corr TD Mask N': ['uncertainty_correlated_time_and_depth_adjustment.mask_false_negative_check_0',
                                            'uncertainty_correlated_time_and_depth_adjustment.mask_false_negative_check_1',
                                            'uncertainty_correlated_time_and_depth_adjustment.mask_false_negative_check_2',
                                            'uncertainty_correlated_time_and_depth_adjustment.mask_false_negative_check_3',
                                            'uncertainty_correlated_time_and_depth_adjustment.mask_false_negative_check_4',
                                            'uncertainty_correlated_time_and_depth_adjustment.mask_false_negative_check_5'],
                     'Unc Corr TD Mask P': ['uncertainty_correlated_time_and_depth_adjustment.mask_false_positive_check_0',
                                            'uncertainty_correlated_time_and_depth_adjustment.mask_false_positive_check_1',
                                            'uncertainty_correlated_time_and_depth_adjustment.mask_false_positive_check_2',
                                            'uncertainty_correlated_time_and_depth_adjustment.mask_false_positive_check_3',
                                            'uncertainty_correlated_time_and_depth_adjustment.mask_false_positive_check_4',
                                            'uncertainty_correlated_time_and_depth_adjustment.mask_false_positive_check_5'],
                     'Unc Rand Mask N': ['uncertainty_random.mask_false_negative_check_0',
                                         'uncertainty_random.mask_false_negative_check_1',
                                         'uncertainty_random.mask_false_negative_check_2',
                                         'uncertainty_random.mask_false_negative_check_3',
                                         'uncertainty_random.mask_false_negative_check_4',
                                         'uncertainty_random.mask_false_negative_check_5'],
                     'Unc Rand Mask P': ['uncertainty_random.mask_false_positive_check_0',
                                         'uncertainty_random.mask_false_positive_check_1',
                                         'uncertainty_random.mask_false_positive_check_2',
                                         'uncertainty_random.mask_false_positive_check_3',
                                         'uncertainty_random.mask_false_positive_check_4',
                                         'uncertainty_random.mask_false_positive_check_5'],
                     'Unc Rand Alt Mask N': ['uncertainty_random_alt.mask_false_negative_check_0',
                                             'uncertainty_random_alt.mask_false_negative_check_1',
                                             'uncertainty_random_alt.mask_false_negative_check_2',
                                             'uncertainty_random_alt.mask_false_negative_check_3',
                                             'uncertainty_random_alt.mask_false_negative_check_4',
                                             'uncertainty_random_alt.mask_false_negative_check_5'],
                     'Unc Rand Alt Mask P': ['uncertainty_random_alt.mask_false_positive_check_0',
                                             'uncertainty_random_alt.mask_false_positive_check_1',
                                             'uncertainty_random_alt.mask_false_positive_check_2',
                                             'uncertainty_random_alt.mask_false_positive_check_3',
                                             'uncertainty_random_alt.mask_false_positive_check_4',
                                             'uncertainty_random_alt.mask_false_positive_check_5'],
                     'Unc Sys Mask N': ['uncertainty_systematic.mask_false_negative_check_0',
                                        'uncertainty_systematic.mask_false_negative_check_1',
                                        'uncertainty_systematic.mask_false_negative_check_2',
                                        'uncertainty_systematic.mask_false_negative_check_3',
                                        'uncertainty_systematic.mask_false_negative_check_4',
                                        'uncertainty_systematic.mask_false_negative_check_5'],
                     'Unc Sys Mask P': ['uncertainty_systematic.mask_false_positive_check_0',
                                        'uncertainty_systematic.mask_false_positive_check_1',
                                        'uncertainty_systematic.mask_false_positive_check_2',
                                        'uncertainty_systematic.mask_false_positive_check_3',
                                        'uncertainty_systematic.mask_false_positive_check_4',
                                        'uncertainty_systematic.mask_false_positive_check_5'],
                     'Unc Sys Alt Mask N': ['uncertainty_systematic_alt.mask_false_negative_check_0',
                                            'uncertainty_systematic_alt.mask_false_negative_check_1',
                                            'uncertainty_systematic_alt.mask_false_negative_check_2',
                                            'uncertainty_systematic_alt.mask_false_negative_check_3',
                                            'uncertainty_systematic_alt.mask_false_negative_check_4',
                                            'uncertainty_systematic_alt.mask_false_negative_check_5'],
                     'Unc Sys Alt Mask P': ['uncertainty_systematic_alt.mask_false_positive_check_0',
                                            'uncertainty_systematic_alt.mask_false_positive_check_1',
                                            'uncertainty_systematic_alt.mask_false_positive_check_2',
                                            'uncertainty_systematic_alt.mask_false_positive_check_3',
                                            'uncertainty_systematic_alt.mask_false_positive_check_4',
                                            'uncertainty_systematic_alt.mask_false_positive_check_5'],
                     'Adjustment Unc Mask N': ['adjustment_uncertainty.mask_false_negative_check_0',
                                               'adjustment_uncertainty.mask_false_negative_check_1',
                                               'adjustment_uncertainty.mask_false_negative_check_2',
                                               'adjustment_uncertainty.mask_false_negative_check_3',
                                               'adjustment_uncertainty.mask_false_negative_check_4',
                                               'adjustment_uncertainty.mask_false_negative_check_5'],
                     'Adjustment Unc Mask P': ['adjustment_uncertainty.mask_false_positive_check_0',
                                               'adjustment_uncertainty.mask_false_positive_check_1',
                                               'adjustment_uncertainty.mask_false_positive_check_2',
                                               'adjustment_uncertainty.mask_false_positive_check_3',
                                               'adjustment_uncertainty.mask_false_positive_check_4',
                                               'adjustment_uncertainty.mask_false_positive_check_5'],
                     'Large Scale Unc Mask N': ['large_scale_correlated_uncertainty.mask_false_negative_check_0',
                                                'large_scale_correlated_uncertainty.mask_false_negative_check_1',
                                                'large_scale_correlated_uncertainty.mask_false_negative_check_2',
                                                'large_scale_correlated_uncertainty.mask_false_negative_check_3',
                                                'large_scale_correlated_uncertainty.mask_false_negative_check_4',
                                                'large_scale_correlated_uncertainty.mask_false_negative_check_5'],
                     'Large Scale Unc Mask P': ['large_scale_correlated_uncertainty.mask_false_positive_check_0',
                                                'large_scale_correlated_uncertainty.mask_false_positive_check_1',
                                                'large_scale_correlated_uncertainty.mask_false_positive_check_2',
                                                'large_scale_correlated_uncertainty.mask_false_positive_check_3',
                                                'large_scale_correlated_uncertainty.mask_false_positive_check_4',
                                                'large_scale_correlated_uncertainty.mask_false_positive_check_5'],
                     'Synoptic Unc Mask N': ['synoptically_correlated_uncertainty.mask_false_negative_check_0',
                                             'synoptically_correlated_uncertainty.mask_false_negative_check_1',
                                             'synoptically_correlated_uncertainty.mask_false_negative_check_2',
                                             'synoptically_correlated_uncertainty.mask_false_negative_check_3',
                                             'synoptically_correlated_uncertainty.mask_false_negative_check_4',
                                             'synoptically_correlated_uncertainty.mask_false_negative_check_5'],
                     'Synoptic Unc Mask P': ['synoptically_correlated_uncertainty.mask_false_positive_check_0',
                                             'synoptically_correlated_uncertainty.mask_false_positive_check_1',
                                             'synoptically_correlated_uncertainty.mask_false_positive_check_2',
                                             'synoptically_correlated_uncertainty.mask_false_positive_check_3',
                                             'synoptically_correlated_uncertainty.mask_false_positive_check_4',
                                             'synoptically_correlated_uncertainty.mask_false_positive_check_5'],
                     'Uncorrelated Unc Mask N': ['uncorrelated_uncertainty.mask_false_negative_check_0',
                                                 'uncorrelated_uncertainty.mask_false_negative_check_1',
                                                 'uncorrelated_uncertainty.mask_false_negative_check_2',
                                                 'uncorrelated_uncertainty.mask_false_negative_check_3',
                                                 'uncorrelated_uncertainty.mask_false_negative_check_4',
                                                 'uncorrelated_uncertainty.mask_false_negative_check_5'],
                     'Uncorrelated Unc Mask P': ['uncorrelated_uncertainty.mask_false_negative_check_0',
                                                 'uncorrelated_uncertainty.mask_false_negative_check_1',
                                                 'uncorrelated_uncertainty.mask_false_negative_check_2',
                                                 'uncorrelated_uncertainty.mask_false_negative_check_3',
                                                 'uncorrelated_uncertainty.mask_false_negative_check_4',
                                                 'uncorrelated_uncertainty.mask_false_negative_check_5']

                     }


class ReportPlotter:
    def __init__(self, usecase, sensor, report, figure_dirpath='.'):
        """

        :type sensor: str
        :type usecase: str
        :type report: dict
        :type figure_dirpath: str
        """
        self.sensor = sensor
        self.usecase = usecase
        self.report = report
        self.figure_dirpath = figure_dirpath

    def get_sensor(self):
        """

        :rtype : str
        """
        return self.sensor

    def get_usecase(self):
        """

        :rtype : str
        """
        return self.usecase

    def get_report(self):
        """

        :rtype : dict
        """
        return self.report

    def get_figure_dirpath(self):
        """

        :rtype : str
        """
        return self.figure_dirpath

    def plot(self):
        self.plot_figure_1()
        self.plot_figure_2()

    def plot_figure_1(self):
        report = self.get_report()
        reference_counts = report['summary_report.count']

        labels = self._extract_labels_plot_1(report)

        plot_title = self.get_usecase().upper() + ' ' + self.get_sensor().replace('_', '-')
        plot_label = 'Failure Permillage (for ' + '{:,}'.format(reference_counts) + ' files in total)'
        filename = self.get_usecase().lower() + '-' + self.get_sensor() + "-figure1.pdf"
        filepath = os.path.join(self.get_figure_dirpath(), filename)
        ReportPlotter.plot_report(report, FIG_1_CHECKS, labels, reference_counts, plot_title, plot_label, filepath)

    def plot_figure_2(self):
        report = self.get_report()
        reference_counts = report['quality_level.count.total']

        labels = self._extract_labels_plot_2(report)
        
        plot_title = self.get_usecase().upper() + ' ' + self.get_sensor().replace('_', '-')
        plot_label = 'Failure Permillage (for ' + '{:,}'.format(reference_counts) + ' pixels in total)'
        filename = self.get_usecase().lower() + '-' + self.get_sensor() + "-figure2.pdf"
        filepath = os.path.join(self.get_figure_dirpath(), filename)
        summary_checks = {}
        summary_checks.update(FIG_2_CHECKS_MIN_MAX)
        summary_checks.update(FIG_2_CHECKS_MASK)
        ReportPlotter.plot_report(report, summary_checks, labels, reference_counts, plot_title, plot_label, filepath,
                                  legend_on=True)

    @staticmethod
    def plot_report(report, checks, check_labels, reference_counts, plot_title, plot_label, filepath, legend_on=False):
        """

        :type report: dict
        :type checks: dict
        :type check_labels: list
        :type reference_counts: int
        :type plot_title: str
        :type plot_label: str
        :type filepath: str
        """
        labels = []
        counts = []
        permillages = {}

        font_label = {'size': 9}
        font_title = {'size': 12}
        office_colors = ['#4572A7', '#AA4643', '#89A54E', '#71588F', '#4198AF', '#DB843D', '#93A9CF', '#D19392']

        for label in reversed(check_labels):
            permillages[label] = []
            total_count = 0
            for check in checks[label]:
                if check in report:
                    count = report[check]
                    permillage = count / (0.001 * reference_counts)
                    total_count += count
                    permillages[label].append(permillage)

            labels.append(label)
            counts.append('{:,}'.format(total_count))

        figure, vertical_axis_l = plt.subplots(figsize=(9.0, 6.0 / 20 * len(labels)))
        plt.subplots_adjust(left=0.25, right=0.85)
        for i, l in enumerate(labels):
            val = permillages[l]
            pos = np.array([i for v in val]) + 0.5
            ReportPlotter._stacked_bar(pos, val, office_colors)
        ticks = np.arange(len(labels)) + 0.5
        plt.yticks(ticks, labels)
        vertical_axis_l.set_title(plot_title, fontdict=font_title)

        vertical_axis_r = vertical_axis_l.twinx()
        vertical_axis_r.set_yticks(ticks)
        vertical_axis_r.set_yticklabels(counts)
        vertical_axis_r.set_ylim(vertical_axis_l.get_ylim())
        vertical_axis_r.set_ylabel('Failure Counts', fontdict=font_label)
        vertical_axis_l.set_ylabel('Checks Conducted', fontdict=font_label)
        vertical_axis_l.set_xlabel(plot_label, fontdict=font_label)

        if legend_on:
            ReportPlotter._create_legend(office_colors[:7])

        figure.savefig(filepath)

    @staticmethod
    def _create_legend(colors):
        patches = [mpatches.Patch(color=colors[0], label='all')]
        for i in range(1, len(colors)):
            patches.append(mpatches.Patch(color=colors[i], label=str(i - 1)))
        plt.legend(handles=patches, ncol=len(colors), loc="lower center", bbox_to_anchor=(0.5, -0.1), frameon=False)

    @staticmethod
    def _stacked_bar(pos, values, colors):
        starts = np.zeros(len(values))
        for i in range(1, len(starts)):
            starts[i] = starts[i - 1] + values[i - 1]
        if len(values) > 1:
            plt.barh(pos, values, left=starts, color=colors[1:], align='center', height=0.5)
        else:
            plt.barh(pos, values, left=starts, color=colors[:1], align='center', height=0.5)

    @staticmethod
    def _extract_labels_plot_1(report):
        return ReportPlotter._extract_labels(FIG_1_CHECKS, report)

    @staticmethod
    def _extract_labels_plot_2(report):
        labels =  ReportPlotter._extract_labels(FIG_2_CHECKS_MIN_MAX, report)
        labels.extend(ReportPlotter._extract_labels(FIG_2_CHECKS_MASK, report))
        return labels

    @staticmethod
    def _extract_labels(checks, report):
        labels = []
        for key, values in checks.items():
            for check in values:
                if check in report:
                    if not key in labels:
                        labels.append(key)
        return sorted(labels)


if __name__ == "__main__":
    # Call with up to two arguments:
    #
    # 1 = usecase
    # 2 = sensor
    # 3 = summary report pathname
    # 4 = figure directory path
    import sys

    argument_count = len(sys.argv)
    if argument_count == 5:
        plotter = ReportPlotter(sys.argv[1], sys.argv[2], ProductVerifier.load_report(sys.argv[3]), sys.argv[4])
    else:
        print('usage:', sys.argv[0], '<usecase> <sensor> <summary report pathname> <figure dirpath>')
        sys.exit(1)

    # noinspection PyBroadException
    try:
        plotter.plot()
    except Exception as e:
        print("Error {0}".format(e.__cause__))
        sys.exit(1)

    sys.exit()
