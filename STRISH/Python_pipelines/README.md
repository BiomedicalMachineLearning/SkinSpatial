# Python process for visualizing cells regional coexpression of genes marker into the serial of windows.

1. Load all the JSON files from directory of detection result. Convert the coordinate of each annotation window from micron values to pixel values.

2. For each window where the cells within it express both target marker in the pair (CSF1R-IL34 or THY1-ITGAM), compute the colexpression score by normalize the number of positive marker with total number of cell. 

3. 
