## STRISH pipeline with Groovy script for detecting cells local co-expression using nonoverlapping windows strategy. 


1. RNAscope with nuclei is colored by DAPI and 3 probes that bind to THY1, IL34, CSF1R colored by Cy3, Cy5, Cy7 respectively (Contrast level of each marker is adjusted to make them more visible).
<a id="step0">![Step 0](/figures/scene1_original_img.png)</a>
2. Apply cell detection using the windows starting with the size of 10% of the image size to search of the areas that contain cells
<a id="step1">![Step 1](/figures/scene1_step1_img.png)</a>
3. The windows with no cell inside are remove while the windows with cells detected are subjected to find local ligand-receptor interaction and kept to find cell local co-expression. 
<a id="step2">![Step 2](/figures/scene1_step2_img.png)</a>
4. Final detection result with the all the windows with cells are detected and possible ligand-receptor local co-expression. The number of cells per window is limited to less 10 cells to preserve local information while warranty enough cell community 
<a id="step3">![LR interation](/figures/scene1_final_img.png )</a>
