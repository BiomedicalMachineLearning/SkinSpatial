SkinSpatial is a repository for the code to analyse skin spatial data. Here we host an official version and development of STRISH (Spatial Transcriptomic and RNA In-situ Hybridization) pipeline. 

# STRISH pipeline documentation 

STRISH is a computational pipeline that enables us to quantitatively model cell-cell interactions by automatically scanning for local expression of RNAscope data to recapitulate an interaction landscape across the whole tissue.

STRISH pipeline consists of three major steps. 


   **Step 1:** Run cell detection for RNAScope data using series of nonoverlapped windows with QuPath. The size of scanning windows are initially set to 10% (cusomizable) of of the whole scan image. If the cell detected result in the current window is greater than the threshold, gradually split the window size into smaller subwindow until the condition is satisfied [window](#window_scan). Otherwise, remove the window from the current list of scanning windows. The cell detection algorithm pseudocode is followed [pseudocode](#pseudocode)   

<a id="pseudocode"><img src="/figures/Pseudocode_STRISH_detection.png" alt="drawing" width="575"/></a>

More illustrations about STRISH cell detection with the windows strategy can be found [here](STRISH/QuPath_pipelines/README.md)
   
   **Step 2:** Run a [Python proces](STRISH/Python_pipelines/RNAscope_CCC_analysis_pipeline.ipynb) to computationally calculate ligand-receptor local co-expression level of two pairs of target markers. For the pair of ligand-receptor which two markes come from two separated scanning image, [image registration](STRISH/Python_pipelines/Images_registration.ipynb) is required before evaluating local co-expression level in the tissue. The final outcome is visualize by the heatmap. 
   
   **Step 3:** Cropping background and plotting tissue contour to focus on the main tissue area, i.e. [Fig 3](#lr_interaction). 
   
## Additional analysis
1. For details about cell-cell interation analysis with Visium data, visit stLearn github page at [stLearn](https://github.com/BiomedicalMachineLearning/stLearn) 
2. For CellPhoneDB method for ligand-receptor interaction prediction, visit CellPhoneDB github page [CellPhoneDB](https://github.com/Teichlab/cellphonedb)
## Examples of STRISH analysis of RNASCope data: 

1. RNAscope scanned image with five markers
<a id="img_regis">![Merged image](/figures/merged_5_channels_image.png)</a>

2. RNAscope scanned image with five markers
<!-- <a id="window_scan">![Cells detection](/figures/scene1_step2_img.png =2x)</a> -->
<a id="window_scan"><img src="/figures/scene1_step2_img.png" alt="drawing"/></a>

3. Heatmap of Ligand-receptor communication for the pair of CSF1R and IL34
<a id="lr_interaction">![LR interation](/figures/collocalization_scene1_CSF1R_IL34.jpg)</a>

 


