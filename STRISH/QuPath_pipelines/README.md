## STRISH pipeline with QuPath script for detecting cells local coexpression using nonoverlapping windows strategy. 


1. The image below shows the original scan image from RNAscope with nuclei is stained by DAPI (blue) and other 3 probes that bind to THY1, IL34, CSF1R are stained  by Cy3 (orange), Cy5 (red) and Cy7 (yellow) fluorophores respectively (Contrast level of each marker is adjusted to make them more visible).
<a id="step0">![Step 0](/figures/scene1_original_img.png)</a>
2. First, cell detection is applied in broad area to search of the areas that possibly contain cells. 
<a id="step1">![Step 1](/figures/scene1_step1_img.png)</a>
3. The windows with zero cell detected are removed while the windows with at least two cells detected are kept and subjected to find local ligand-receptor interaction in smaller windows.  
<a id="step2">![Step 2](/figures/scene1_step2_img.png)</a>
4. Final detection result with the all the windows which cells are detected and possible ligand-receptor local co-expression. The threshold for the number of cell for each window is set to 10 cells to make sure that the computation effort is not too expensive while the subsequent steps with Python script produce enough detail  about cells local coexpression.     
<a id="step3">![LR interation](/figures/scene1_final_img.png )</a>
