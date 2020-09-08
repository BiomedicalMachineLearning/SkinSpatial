import glob
import io
from ipywidgets import interact
from ipywidgets import fixed
from IPython.display import clear_output
import matplotlib.pyplot as plt
import numpy as np
import os
import re
from scipy import ndimage as ndi
from pathlib import Path
import pandas as pd
from PIL import Image, ImageOps, ImageChops
import seaborn as sns
import json
import SimpleITK as sitk
from collections import OrderedDict
from skimage.exposure import histogram
from skimage.transform import rotate
from skimage.color import rgb2gray, rgb2hed
from skimage import measure
import time
import warnings
import cv2
from scipy.optimize import minimize, rosen, rosen_der
import collections



def get_files_in_directory(directory, postfix=""):
    """ list all the files with postfix in the directory and return the sorted list """
    fileNames = [s for s in os.listdir(directory) if not os.path.isdir(os.path.join(directory, s))]
    if not postfix or postfix == "":
        return sorted(fileNames)
    else:
        return sorted([s for s in fileNames if s.lower().endswith(postfix)])
    
def get_files_in_dir_recursively(directory, postfix='json'):
    """ list all the files in the directory with postfix recursively """
    files = [file for file in glob.glob(os.path.join(directory, '**/*.{0}'.format(postfix)), recursive=True)]
    return files

def get_subdirectories_in_directory(directory, postfix=""):
    """ list all the subdirectories in the directory """
    dir_names = [s for s in os.listdir(directory) if os.path.isdir(os.path.join(directory, s))]
    return sorted(dir_names)

def mkdirs(dirs):
    """ create new directory  if it does not exist"""
    if not os.path.exists(dirs):
        os.makedirs(dirs)

def convert_original2scaled(x_coord, y_coord, scaled_size, original_size=(54513, 23814), exclude_rate_h=1.0, exclude_rate_w=1.0):
    """ convert the annotation box coordinate from micron unit to pixel unit"""
    return int(x_coord*scaled_size[0]/(original_size[0]*exclude_rate_w)), int(y_coord*scaled_size[1]/(original_size[1]*exclude_rate_h))


def list_to_int(list1D):
    """ convert list of float values to integer values """
    return [int(float(x)) for x in list1D]

def draw_rectangles(img, rects, color=(0, 255, 255), thickness=7):
    """ draw rectangles to a cv2 img 
    img: cv2 image in 2D/3D array
    rects: list of rectangle coordinates top, left, bottom, right
    color: rectangle color
    thickness: the thickness of 4 edges
    """
    clone_image = img.copy()
    for rect in rects:
        pt1 = tuple(list_to_int(rect[0:2]))
        pt2 = tuple(list_to_int(rect[2:]))
        cv2.rectangle(clone_image, pt1, pt2, color, thickness, cv2.FILLED)
    return clone_image

def get_xy_from_fn(fn):
    """ parse file name to extract rectangle coordinates
    fn: filename string
    """
    results = re.findall('x(\d+)_y(\d+)', fn)
    if len(results) == 1:
        x_coord, y_coord = results[0]
        x_coord, y_coord = int(x_coord), int(y_coord)
        return x_coord, y_coord, None
    else:
        x_coord, y_coord = results[-1]
        x_coord, y_coord = int(x_coord), int(y_coord)
        return x_coord, y_coord, [list_to_int(element) for element in results[:-1]]

def substitute_file_name(file_name, pattern = 'Cy7',sub_string='Cy5'):
    """ substitute one string with pattern by a new substring """
    output = re.sub(pattern, sub_string, file_name)
    return output 

def thumbnail(img, size = (1000,1000)):
    """ create a thumbnail image """
    img_thumbnail = img.copy()
    img_thumbnail.thumbnail(size)
    return img_thumbnail

def overlay_pil_imgs(foreground, background, best_loc = (0,0), alpha=0.5):
    """ overlay two images to visualize the registration """
    newimg1 = Image.new('RGBA', size=background.size, color=(0, 0, 0, 0))
    newimg1.paste(foreground, best_loc)
    newimg1.paste(background, (0, 0))

    newimg2 = Image.new('RGBA', size=background.size, color=(0, 0, 0, 0))
    newimg2.paste(background, (0, 0))
    newimg2.paste(foreground, best_loc)
    result = Image.blend(newimg1, newimg2, alpha=alpha)
    return result

def transform_point(transform, point):
    transformed_point = transform.TransformPoint(point, )
    return transformed_point
#     print('Point ' + str(point) + ' transformed is ' + str(transformed_point))

def convert_rect_coords(transform, rect):
    """ convert the coordinate of a rectangle using a transformation method """
    x1, y1, x2, y2 = rect
    transformed_x1, transformed_y1 = transform_point(transform, (x1, y1))
    transformed_x2, transformed_y2 = transform_point(transform, (x2, y2))
    transformed_rect = (transformed_x1, transformed_y1, transformed_x2, transformed_y2)
    return transformed_rect


def y_element(element):
    return element[0]
def x_element(element):
    return element[1]
def xy_element(element):
    return element[0]+ element[1]

def get_itk_from_pil(pil_img):
    """Converts Pillow image into ITK image
    """
    return sitk.GetImageFromArray(np.array(pil_img))

def get_pil_from_itk(itk_img):
    """Converts ITK image into Pillow Image
    """
    return Image.fromarray(sitk.GetArrayFromImage(itk_img).astype(np.uint8))

def show_alignment(fixed_img, moving_img, prefilter = None):
    """Visualises alignment of fixed image with moving image
    
    Fixed image is displayed as blue
    Moving image is displayed as pink 
    """
    if prefilter == 'TP53':
        tp53_filtered = filter_green(moving_img)
        tp53_filtered = filter_grays(tp53_filtered, tolerance = 3)
        moving_img = filter_otsu_global(tp53_filtered, 'PIL')
        he_filtered = filter_green(fixed_img)
        he_filtered = filter_grays(he_filtered, tolerance = 15)
        fixed_img = filter_otsu_global(he_filtered, 'PIL')
    background = (255,255,255)
    img_red = ImageOps.colorize(moving_img.convert('L'), (255, 0, 0), background)
    img_blue = ImageOps.colorize(fixed_img.convert('L'), (0, 0, 255), background)
    img_red.putalpha(120)
    img_blue.putalpha(70)
    return Image.alpha_composite(img_red, img_blue)

def sitk_transform_rgb(moving_rgb_img, fixed_rgb_img, transform, interpolator = sitk.sitkLanczosWindowedSinc):
    """Applies a Simple ITK transform (e.g. Affine, B-spline) to an RGB image
    
    The transform is applied to each channel
    
    Parameters
    ----------
    moving_rgb_img : Pillow Image 
        This image will be transformed to produce the output image
    fixed_rgb_img : Pillow Image
        This reference image provides the output information (spacing, size, and direction) of the output image
    transform : SimpleITK transform
        Generated from image registration
    interpolator : SimpleITK interpolator
    
    Returns
    -------
    rgb_transformed : Pillow Image
        Transformed moving image 
    """
    transformed_channels = []
    r_moving, g_moving, b_moving, = moving_rgb_img.convert('RGB').split()
    r_fixed, g_fixed, b_fixed = fixed_rgb_img.convert('RGB').split()
    for moving_img, fixed_img in [(r_moving, r_fixed), (g_moving, g_fixed), (b_moving, b_fixed)]:
        moving_img_itk = get_itk_from_pil(moving_img)
        fixed_img_itk = get_itk_from_pil(fixed_img)
        transformed_img = sitk.Resample(moving_img_itk, fixed_img_itk, transform, 
                            interpolator, 0.0, moving_img_itk.GetPixelID())
        transformed_channels.append(get_pil_from_itk(transformed_img))
    rgb_transformed = Image.merge('RGB', transformed_channels)
    return rgb_transformed    

def start_plot():
    """Setup data for plotting
    
    Invoked when StartEvent happens at the beginning of registration.
    """
    global metric_values, multires_iterations
    
    metric_values = []
    multires_iterations = []

def end_plot():
    """Cleanup the data and figures 
    """
    global metric_values, multires_iterations
    
    del metric_values
    del multires_iterations
    # Close figure, we don't want to get a duplicate of the plot latter on.
    plt.close()

# Callback invoked when the IterationEvent happens, update our data and display new figure.
def plot_values(registration_method):
    global metric_values, multires_iterations
    
    metric_values.append(registration_method.GetMetricValue())                                       
    # Clear the output area (wait=True, to reduce flickering), and plot current data
    clear_output(wait=True)
    # Plot the similarity metric values
    plt.plot(metric_values, 'r')
    plt.plot(multires_iterations, [metric_values[index] for index in multires_iterations], 'b*')
    plt.xlabel('Iteration Number',fontsize=12)
    plt.ylabel('Metric Value',fontsize=12)
    plt.show() 
    
def update_plot(registration_method):
    """Plot metric value after each registration iteration
    
    Invoked when IterationEvent happens.
    """
    global metric_values, multires_iterations
    
    metric_values.append(registration_method.GetMetricValue())                                       
    # Clear the output area (wait=True, to reduce flickering), and plot current data
    clear_output(wait=True)
    # Plot the similarity metric values
    plt.plot(metric_values, 'r')
    plt.plot(multires_iterations, [metric_values[index] for index in multires_iterations], 'b*')
    plt.xlabel('Iteration Number', fontsize=12)
    plt.ylabel('Metric', fontsize=12)
    plt.show()
    
def update_multires_iterations():
    """Update the index in the metric values list that corresponds to a change in registration resolution
    
    Invoked when the sitkMultiResolutionIterationEvent happens.
    """
    global metric_values, multires_iterations
    multires_iterations.append(len(metric_values))
    
def plot_metric(title = 'Plot of registration metric vs iterations'):
    """Plots the mutual information over registration iterations
    
    Parameters
    ----------
    title : str
    
    Returns
    -------
    fig : matplotlib figure
    """
    global metric_values, multires_iterations
    
    fig, ax = plt.subplots()
    ax.set_title(title)
    ax.set_xlabel('Iteration Number', fontsize=12)
    ax.set_ylabel('Mutual Information Cost', fontsize=12)
    ax.plot(metric_values, 'r')
    ax.plot(multires_iterations, [metric_values[index] for index in multires_iterations], 'b*', label = 'change in resolution')
    ax.legend()
    return fig

################################
# Mutual Information Functions #
################################

def mutual_information(hgram):
    """Mutual information for joint histogram
    """
    # Convert bins counts to probability values
    pxy = hgram / float(np.sum(hgram))
    px = np.sum(pxy, axis = 1) # marginal for x over y
    py = np.sum(pxy, axis = 0) # marginal for y over x
    px_py = px[:, None] * py[None, :] #Broadcat to multiply marginals
    # Now we can do the calculation using the pxy, px_py 2D arrays
    nzs = pxy > 0 # Only non-zero pxy values contribute to the sum
    return np.sum(pxy[nzs] * np.log(pxy[nzs] / px_py[nzs]))

def mutual_info_histogram(fixed_img, moving_img, bins = 20, log = False):
    hist_2d, x_edges, y_edges = np.histogram2d(fixed_img.ravel(), moving_img.ravel(), bins = bins)
    if log:
        hist_2d_log = np.zeros(hist_2d.shape)
        non_zeros = hist_2d != 0
        hist_2d_log[non_zeros] = np.log(hist_2d[non_zeros])
        return hist_2d_log
    return hist_2d

def plot_mutual_info_histogram(histogram):
    plt.imshow(histogram.T, origin = 'lower')
    plt.xlabel('Fixed Image')
    plt.ylabel('Moving Image')

def calculate_mutual_info(fixed_img, moving_img):
    hist = mutual_info_histogram(fixed_img, moving_img)
    return mutual_information(hist)

def map_heat_values2colors(values):
    colors = list()
    sorted_values = np.sort(values, kind='mergesort')
    set_box_scores = list(OrderedDict.fromkeys(sorted_values).keys())
    heat_colors_range = sns.color_palette('viridis', len(set_box_scores))
    for value in values:
        if value == 0:
            raw_color = [0,0,0]
        else:
            raw_color = heat_colors_range[set_box_scores.index(value)]
        colors.append(raw_color)
    return np.array(colors)

def draw_rectangles_heat(img, rects, colors, scores):
    clone_image = img.copy()
    for index, rect in enumerate(rects):
        pt1 = tuple(list_to_int(rect[0:2]))
        pt2 = tuple(list_to_int(rect[2:]))
        cv2.rectangle(clone_image, pt1, pt2, colors[index], cv2.FILLED)
    return clone_image

def collocalize_score(fn):
    equivalent_cy7_fn = substitute_file_name(fn, 'DAPI', 'Cy7')
    equivalent_cy5_fn = substitute_file_name(fn, 'DAPI', 'Cy5')
    with open(fn) as json_file:
        dapi_json = json.load(json_file)
    cell_counts = len(dapi_json)
    if os.path.exists(equivalent_cy7_fn) and os.path.exists(equivalent_cy5_fn):
        try:
            with open(equivalent_cy7_fn) as json_file:
                cy7_json = json.load(json_file)
            present_cy7 = len(cy7_json)
        except:
            present_cy7 = 0
        try: 
            with open(equivalent_cy5_fn) as json_file:
                cy5_json = json.load(json_file)
            present_cy5 = len(cy5_json)
        except: 
            present_cy5 = 0  
    if present_cy5 == 0 or present_cy7 == 0:
        return 0#, 0

    return (present_cy5+present_cy7)/cell_counts

def area_of_rect(rect):
    x1, y1, x2, y2 = rect
    return abs(x2-x1)*abs(y2-y1)


def parse_measurement_string(measurement_string):
    result = re.sub(r"\[|\]", '',measurement_string)
    elements = result.split(', ')
    data_dict = collections.OrderedDict()
    for element in elements:
        float_value = re.findall("-?\d+\.\d+|NaN", element)
        if len(float_value) == 0:
            key_value, name = re.split(' ', element)
            data_dict[key_value[:-1]] = name
        else:
            exponents = re.findall("([eE][-+]?[0-9]+)", element)
            if len(exponents) > 0:
                key_value = re.sub(str(float_value[0])+str(exponents[0]), '', element)
                data_dict[key_value[:-2]] = float(str(float_value[0])+str(exponents[0]))
            else:
                key_value = re.sub(float_value[0], '', element)
                if float_value[0] == 'NaN':
                    data_dict[key_value[:-2]] = float(0.0)
                else:
                    data_dict[key_value[:-2]] = float(float_value[0])

    return pd.DataFrame(data_dict, index=[0]), data_dict.keys()

def convert_json2dataframe(fn, col_names = list()):
    with open(fn) as json_file:
        json_values = json.load(json_file)

    if len(json_values) != 0:
        try:
            measure_values, row_name = parse_measurement_string(json_values[0])
        except:
            print(fn)
        for element in json_values[1:]:
            frame, keys = parse_measurement_string(element)
            measure_values = measure_values.append(frame, ignore_index=True)
        return measure_values
    else:
        if len(col_names) == 0:
            raise Exception("No value found", fn)
        else:
            measure_values = pd.DataFrame(columns= col_names)
            return measure_values
        
def inverse_transform_point(xform, p):
    """
    Returns the inverse-transform of a point.

    :param sitk.Transform xform: The transform to invert
    :param (float,float)|[float|float]|np.ndarray p: The point to find inverse for
    :return np.ndarray, bool: The point and whether the operation succeeded or not
    """

    def fun(x):
        return np.linalg.norm(xform.TransformPoint(x) - p)

    p = np.array(p)
    res = minimize(fun, p, method='Powell')
    return res.x, res.success

def convert_rect_coords(transform, rects):
    x1, y1, x2, y2 = rects
    top_left, state_1 = inverse_transform_point(transform, (x1, y1))
    bottom_right, state_2 = inverse_transform_point(transform, (x2, y2))
    transformed1_x1, transformed1_y1 = top_left
    transformed1_x2, transformed1_y2 = bottom_right
    transformed_rect = (transformed1_x1, transformed1_y1, transformed1_x2, transformed1_y2)
    return transformed_rect

def rotate_contour(contour_points, angle, center):
    new_ct_pts = np.zeros_like(contour_points)
    for index, points in enumerate(contour_points):
        x, y = points
        new_y = (y - center[1])*np.cos(np.pi*angle/180) + (x - center[0])*np.sin(np.pi*angle/180) + center[1]
        new_x = -(y- center[1])*np.sin(np.pi*angle/180) + (x- center[0])*np.cos(np.pi*angle/180) + center[0]
        new_y = new_y - (center[1] -  center[0])
        new_x = new_x +  (center[1] -  center[0])
        new_ct_pts[index] = np.array([np.abs(new_x), np.abs(new_y)])
    return new_ct_pts