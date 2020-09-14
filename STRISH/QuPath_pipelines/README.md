STRISH detection flow. 


1. RNAscope with 3 probes that bind to THY1, IL34, CSF1R colored by Cy3, Cy5, Cy7 respectively 
<a id="step0">![Step 0](/figures/scene1_original_img.png)</a>
2. Apply cell detection in 10% of the image size to search of the areas that have cells
<a id="step1">![Step 1](/figures/scene1_step1_img.png){ width=75% }</a>
3. The windows with no cell inside are remove while the windows with cells detected are subjected to find local ligand-receptor interaction  
<a id="step2">![Step 2](/figures/scene1_step2_img.png){ width=75% }</a>
4. Final detection result with the all the windows with cells are detected and possible ligan-recept local co-expression. 
<a id="step3">![LR interation](/figures/scene1_final_img.png ){ width=75% }</a>
