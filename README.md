SkinSpatial is a repository for the code to analyse skin spatial data. Here we host an official version and development of STRISH (Spatial Transcriptomic and RNA In-situ Hybridization) pipeline. 

# STRISH pipeline documentation 

STRISH is a computational pipeline that enables us to quantitatively model cell-cell interactions by automatically scanning for local expression of RNAscope data to recapitulate an interaction landscape across the whole tissue.

1. To process RNAScope data, see the Ipynb at [STRISH/Python_pipeline/Image_](STRISH/Python_pipelines/RNAscope_CCC_analysis_pipeline.ipynb)
2. To run the cell detection pipeline copy the groovy files froom [STRISH/QuPath_pipelines](STRISH/QuPath_pipelines/)
3. For details about image resgistration, see the Ipynb at [STRISH/Python_pipeline](STRISH/Python_pipelines/Images_registration.ipynb)
4. For details about cell-cell interation analysis with Visium data, visit stLearn page at [stLearn](https://github.com/BiomedicalMachineLearning/stLearn) 

## Examples of STRISH analysis of RNASCope data: 

1. RNAscope scanned image
![Merged image](/figures/merged_5_channels_image.png)

2. Heatmap of Ligand-receptor communication for the pair of CSF1R and IL34 
![LR interation](/figures/collocalization_scene1_CSF1R_IL34.jpg)

