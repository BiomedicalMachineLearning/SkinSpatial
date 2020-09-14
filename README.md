SkinSpatial is a repository for the code to analyse skin spatial data. Here we host an official version and development of STRISH (Spatial Transcriptomic and RNA In-situ Hybridization) pipeline. 

# STRISH pipeline documentation 

STRISH is a computational pipeline that enables us to quantitatively model cell-cell interactions by automatically scanning for local expression of RNAscope data to recapitulate an interaction landscape across the whole tissue.

STRISH pipeline consists of three major steps. 


   **Step 1:** Run cell detection using series of nonoverlapped windows with QuPath. For generality, the size of scanning windows are initially set to 10% of of the whole scan image. Then gradually the detection window sizes are scaled down until the number of cell detected in each window are less than a threshold. More specific information can be found [here](STRISH/QuPath_pipelines/README.md)
   
   **Step 2:** Run a Python process to computationally cell local co-expression level of two pairs of Ligand Receptor. For the pair of ligand-receptor which target markes come from two separated scanning image, image registration is required before evaluating local co-expression level in the tissue. The final outcome is visualize by the heatmap. 
   
   **Step 3:** Background cropping and plotting tissue contour to focus on the main tissue area, i.e. [Fig 2](#lr_interaction). 
   
1. To process RNAScope data, see the Ipynb at [STRISH/Python_pipeline/CCC_analysis_pipeline](STRISH/Python_pipelines/RNAscope_CCC_analysis_pipeline.ipynb)
2. To run the cell detection pipeline create a QuPath project and copy the groovy script file the script directory. More detail in [STRISH/QuPath_pipelines](STRISH/QuPath_pipelines/README.md)
3. For details about image resgistration, see the Ipynb at [STRISH/Python_pipeline/Image_registation](STRISH/Python_pipelines/Images_registration.ipynb)
4. For details about cell-cell interation analysis with Visium data, visit stLearn page at [stLearn](https://github.com/BiomedicalMachineLearning/stLearn) 

## Examples of STRISH analysis of RNASCope data: 

1. RNAscope scanned image
![Merged image](/figures/merged_5_channels_image.png)


<a id="lr_interaction">![LR interation](/figures/collocalization_scene1_CSF1R_IL34.jpg)</a>
2. Heatmap of Ligand-receptor communication for the pair of CSF1R and IL34
 


