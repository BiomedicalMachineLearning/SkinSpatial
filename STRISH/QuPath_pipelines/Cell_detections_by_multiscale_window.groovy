print('Start Here')
// / Set the parameter 
// CD207, NM_015717.5 – T1 channel -> FITC (green)
// Thy1, NM_001311160.2, T3 channel -> Cy3  (orange)
// IL34, NM_001172771.2, T2 channel -> Cy5 (far red)
// CSF1R, NM_001288705.2, T1 channel -> Cy7 (yellow)
// Itgam, NM_000632.4, T1 channel -> Cy5 (red)
// co-localized analysis CSF1R and IL34 
// co-localized analysis CD207 and Thy1
// 1000 cells in one grid
// if 100 cells per grid 
import static qupath.lib.gui.scripting.QPEx.*
import qupath.lib.regions.ImagePlane
import qupath.lib.display.ChannelDisplayInfo
import qupath.lib.display.ImageDisplay
import qupath.lib.objects.PathCellObject
import qupath.lib.roi.RectangleROI
import qupath.lib.images.servers.ImageChannel
import qupath.lib.objects.hierarchy.PathObjectHierarchy
import qupath.lib.images.servers.bioformats.BioFormatsImageServer
import qupath.lib.objects.PathAnnotationObject
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonElement
import javafx.collections.FXCollections
import java.util.HashMap

import java.awt.Color
import java.awt.image.BufferedImage
import java.awt.image.IndexColorModel
import javax.imageio.ImageIO
import qupath.lib.regions.RegionRequest
// import static qupath.lib.roi.PathROIToolsAwt.getShape

def export_image_display_json(String imag_display_json, FXCollections.UnmodifiableObservableListImpl channels_info,boolean prettyPrint = true) {
  JsonParser parser = new JsonParser()
  JsonArray array = new JsonArray()
  parsed_result = parser.parse(imag_display_json)
  print(parser.getClass())
  if (parsed_result.isJsonArray()) {
    for(index =0; index < parsed_result.size(); index ++) {
      JsonObject json_obj = parsed_result.get(index)
      json_obj.addProperty("maxAllowed", channels_info[index].getMaxAllowed())
      json_obj.addProperty('minAllowed', channels_info[index].getMinAllowed())
      array.add(json_obj)
    }
  }
  if (prettyPrint) {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      return gson.toJson(array);
  } else
      return array.toString();
}

def get_json_from_array(ArrayList<String> arr) {
    Gson g = new GsonBuilder().setPrettyPrinting().create()

    String str = g.toJson(arr)
    return str
}

def name_the_annot(RectangleROI annot,ImageChannel channelName) {
  def annot_w = annot.x2 - annot.x
  def annot_h = annot.y2 - annot.y
  String file_name = String.format("%s_annot_block_x%.0f_y%.0f_w%.0f_h%.0f", channelName.getName(),annot.x,annot.y, annot_w, annot_h)
  return file_name
}

def change_annotation_color(ArrayList<PathCellObject> detections_info, Color color = getColorRGB(0, 200, 255)) {
  for(detection in detections_info){
    detection.setColorRGB(color)
  }
}

def export_detector_cell_json(ArrayList<PathCellObject> detections_info, String channel_name, String save_path) {

  ArrayList<String> measurement_values = new ArrayList<String>()
  // print(detections_info)
  for (int c = 0; c < detections_info.size(); c++) {
    // print(detections_info[c].getPolygonPoints())
    String current_str = String.format("Name: %s, ROI: %s, Centroid X: %s, Centroid Y: %s, %s",detections_info[c].getPathClass(), detections_info[c].getNucleusROI().getRoiName(), detections_info[c].getNucleusROI().getCentroidX(), detections_info[c].getNucleusROI().getCentroidY(), detections_info[c].getMeasurementList().toString())
    measurement_values.add(current_str)
  }
  measure_json = get_json_from_array(measurement_values)
  String filename =  String.format('%s/measurement_values_%s.json',save_path,channel_name)
  File json_before = new File(filename)
  json_before.write(measure_json)
}

def add_annotation_boxes(int image_w, int image_h, ImagePlane image_plane, int current_x_coord = 0, int current_y_coord = 0, float roi_rate = 0.1) {
  int bound = Math.round(1/roi_rate)
  ArrayList<PathObjects> added_objects = new ArrayList<PathObjects>()
  int roi_w = (int)image_w*roi_rate
  int roi_h = (int)image_h*roi_rate
  for(int index_w = 0; index_w < bound; index_w++) {
  for(int index_h = 0; index_h < bound; index_h++) {
    new_x_coord = index_w*roi_h
    new_y_coord = index_h*roi_w
    def current_roi = ROIs.createRectangleROI(current_y_coord+new_y_coord, current_x_coord+new_x_coord, roi_w, roi_h, image_plane)
    def current_annot = PathObjects.createAnnotationObject(current_roi)
    addObject(current_annot)
    added_objects.add(current_annot)
  }
  // def logger = String.format("Current roi top left is: %d %d ",current_x_coord, current_y_coord)
  // print(logger)
  }
  return added_objects
}

def gen_plugin_command(String channel_name, HashMap params) {
  String plugin_param = String.format('{"detectionImage": %s,  "requestedPixelSizeMicrons": %f,  "backgroundRadiusMicrons": %f,  "medianRadiusMicrons": %f,  "sigmaMicrons": %f,  "minAreaMicrons": %f,  "maxAreaMicrons": %f,  "threshold": %f,  "watershedPostProcess": %b,  "cellExpansionMicrons": %f,  "includeNuclei": true,  "smoothBoundaries": true,  "makeMeasurements": true,  "thresholdCompartment": "Nucleus: Cy7 mean",  "thresholdPositive1": 100.0,  "thresholdPositive2": 200.0,  "thresholdPositive3": 300.0,  "singleThreshold": true}',channel_name, params.get('requestedPixel'), params.get('bgradius'), params.get('medianRadiusMicrons'), params.get('sigmaMicrons'), params.get('minArea'), params.get('maxArea'),  params.get('intensityThreshold'),  params.get("watershedPostProcess"),  params.get("cellExpansionMicrons"))
  return plugin_param
}

def run_detection_for_window(BioFormatsImageServer server_func, HashMap map_channel_index, HashMap target_marker, String marker_name, String scene_number, PathAnnotationObject current_annot,ImageDisplay image_display) {
    def channel_index = map_channel_index.get(marker_name)
    def channel_name = server_func.getChannel(channel_index)
    def path_name = String.format('%s/%s',scene_number, channel_name.getName())
    def path_output = buildFilePath(QPEx.PROJECT_BASE_DIR, path_name)
    mkdirs(path_output)
    image_display.setChannelSelected(image_display.availableChannels()[channel_index], true)
    // all_channels[channel_index].setMinDisplay(map_channel_index.get('minDisplay'))
    // all_channels[channel_index].setMaxDisplay(map_channel_index.get('maxDisplay'))
    getCurrentHierarchy().getSelectionModel().setSelectedObject(current_annot) 
    detections = getDetectionObjects()
    removeObjects(detections, false)
    String plugin_params = gen_plugin_command(channel_name.getName(), target_marker)
    runPlugin('qupath.imagej.detect.cells.PositiveCellDetection', plugin_params)
    detections_result = getDetectionObjects()
    def result = new HashMap()
    result.put('detections_result', detections_result)
    result.put('path', path_output)
    result.put('channel_name', channel_name.getName())
    return result
}

def viewer = getCurrentViewer()
def image_data = viewer.getImageData()
def server = image_data.getServer()

//----------------clean up all annotation from image (if any), add new annotations image 
// remove list of PathObject and their children in hierachy setting
detections = getDetectionObjects()
existing_annotation = getAnnotationObjectsAsArray()
// print(existing_annotation.getClass())
removeObjects(detections, false)
removeObjects(existing_annotation, false)

// print(annotations.getClass())
// --------------------------------
print('Added annotations')
// Set the parameter 
//----------------------------------------------
def cy7_params = new HashMap()
cy7_params.put("minDisplay", new Float(157.0))
cy7_params.put("maxDisplay", new Float(382.0))
cy7_params.put("requestedPixel", new Float(0.05))
cy7_params.put("bgradius", new Float(1.0))
// control threshold for the detected object shape
cy7_params.put("medianRadiusMicrons", new Float(0.0001))
cy7_params.put("sigmaMicrons", new Float(0.5))
cy7_params.put("minArea", new Float(0.1))
cy7_params.put("maxArea", new Float(20.0))
cy7_params.put("intensityThreshold", new Float(15.0))
cy7_params.put("watershedPostProcess", new Boolean(false))
// set it low because it's RNA not cell, just make use of detection function
cy7_params.put('cellExpansionMicrons', new Float(0.5))

def cy5_params = new HashMap()
// param for round 1 and 2 are the same
cy5_params.put("minDisplay", new Float(150.0))
cy5_params.put("maxDisplay", new Float(420.0))
cy5_params.put("requestedPixel", new Float(0.05))
cy5_params.put("bgradius", new Float(1.0))
// control threshold for the detected object shape
cy5_params.put("medianRadiusMicrons", new Float(0.0001))
cy5_params.put("sigmaMicrons", new Float(0.5))
cy5_params.put("minArea", new Float(0.1))
cy5_params.put("maxArea", new Float(20.0))
cy5_params.put("intensityThreshold", new Float(37.5))
cy5_params.put("watershedPostProcess", new Boolean(false))
// set it low because it's RNA not cell, just make use of detection function
cy5_params.put('cellExpansionMicrons', new Float(0.5))

def cy3_params = new HashMap()
cy3_params.put("minDisplay", new Float(90.0))
cy3_params.put("maxDisplay", new Float(400.0))
cy3_params.put("requestedPixel", new Float(0.05))
cy3_params.put("bgradius", new Float(1.0))
// control threshold for the detected object shape
cy3_params.put("medianRadiusMicrons", new Float(0.0001))
cy3_params.put("sigmaMicrons", new Float(0.5))
cy3_params.put("minArea", new Float(0.1))
cy3_params.put("maxArea", new Float(30.0))
cy3_params.put("intensityThreshold", new Float(25.0))
cy3_params.put("watershedPostProcess", new Boolean(false))
// set it low because it's RNA not cell, just make use of detection function
cy3_params.put('cellExpansionMicrons', new Float(0.5))


def dapi_params = new HashMap()
dapi_params.put("minDisplay", new Float(60.0))
dapi_params.put("maxDisplay", new Float(6000.0))
dapi_params.put("requestedPixel", new Float(0.25))
dapi_params.put("bgradius", new Float(1.5))
// control threshold for the detected object shape
dapi_params.put("medianRadiusMicrons", new Float(0))
dapi_params.put("sigmaMicrons", new Float(1.5))
dapi_params.put("minArea", new Float(10.0))
dapi_params.put("maxArea", new Float(100.0))
dapi_params.put("intensityThreshold", new Float(50.0))
dapi_params.put("watershedPostProcess", new Boolean(true))
dapi_params.put('cellExpansionMicrons', new Float(2))

def channel2index = new HashMap()
for (int c = 0; c < server.nChannels(); c++) {
  def channelName = server.getChannel(c.intValue())
  print(String.format('Channel Name: %s, index: %d',channelName.getName(),c.intValue()))
  channel2index.put(channelName.getName(), new Integer(c.intValue()))
}
print(channel2index.get('DAPI'))
print(channel2index.get('Cy3'))

def image_display = viewer.getImageDisplay()

def channels = image_display.availableChannels()
// print(channels.getClass())
// print(getCurrentHierarchy().getClass()) 
current_scene_number = '1005_Scene_2_R1_threshold_20c'
def base_path = buildFilePath(QPEx.PROJECT_BASE_DIR, current_scene_number)
mkdirs(base_path)
int count_cell = 11

int z = 0
int t = 0
int image_width = server.getWidth() 
int image_height = server.getHeight() 

def plane = ImagePlane.getPlane(z, t)
int counter = 0
// Add the grid of ROI into image
def annotations =  add_annotation_boxes(image_width, image_height, plane, 0, 0, 0.125)
def threshold = 20
existing_annotations = getAnnotationObjectsAsArray()

while (count_cell > threshold || existing_annotations.size() > 0 ) {
    existing_annotations = getAnnotationObjectsAsArray()
    print('-------------------------------------')
    print(existing_annotations.size())
    print('-------------------------------------')
//    1/0
    counter += 1
    for (annot in existing_annotations) {
        dapi_detection_resuls = run_detection_for_window(server, channel2index, dapi_params, 'DAPI', current_scene_number , annot, image_display)
        print('Numb of nuclei detected :'+ dapi_detection_resuls.get('detections_result').size())
        count_cell = dapi_detection_resuls.get('detections_result').size()
        if(count_cell <= 3) {
          removeObject(annot, false)
          // Do nothing from here
        } 
        else if( (count_cell > 3) &&  (count_cell <= threshold)) {
            print('Record this line for dectecting other marker')
            // remove it from current list to avoid duplicate
            def sub_image_width = annot.getROI().x2 - annot.getROI().x
            def sub_image_height = annot.getROI().y2 - annot.getROI().y
            String file_name = String.format("%s_annot_block_x%.0f_y%.0f_w%.0f_h%.0f", dapi_detection_resuls.get('channel_name'), annot.getROI().getAllPoints()[0].getX(), annot.getROI().getAllPoints()[0].getY(),sub_image_width,sub_image_height)
            export_detector_cell_json(dapi_detection_resuls.get('detections_result'), file_name, dapi_detection_resuls.get('path'))
            
            cy7_channel_index = channel2index.get('Cy7')
            channels[cy7_channel_index].setMinDisplay(cy7_params.get('minDisplay'))
            channels[cy7_channel_index].setMaxDisplay(cy7_params.get('maxDisplay'))
            cy7_results = run_detection_for_window(server, channel2index, cy7_params, 'Cy7', current_scene_number, annot, image_display)
            print('Numb of cy7 detected :'+ cy7_results.get('detections_result').size())
      
            String file_name_cy7 = String.format("%s_annot_block_x%.0f_y%.0f_w%.0f_h%.0f", cy7_results.get('channel_name'),annot.getROI().getAllPoints()[0].getX(),annot.getROI().getAllPoints()[0].getY(),sub_image_width,sub_image_height)
            export_detector_cell_json(cy7_results.get('detections_result'), file_name_cy7, cy7_results.get('path'))
      
            cy5_channel_index = channel2index.get('Cy5')
            channels[cy5_channel_index].setMinDisplay(cy5_params.get('minDisplay'))
            channels[cy5_channel_index].setMaxDisplay(cy5_params.get('maxDisplay'))
            cy5_results = run_detection_for_window(server, channel2index, cy5_params, 'Cy5', current_scene_number, annot, image_display)
            print('Numb of cy5 detected :'+ cy5_results.get('detections_result').size())
      
            String file_name_cy5 = String.format("%s_annot_block_x%.0f_y%.0f_w%.0f_h%.0f", cy5_results.get('channel_name'),annot.getROI().getAllPoints()[0].getX(),annot.getROI().getAllPoints()[0].getY(),sub_image_width,sub_image_height)
            export_detector_cell_json(cy5_results.get('detections_result'), file_name_cy5, cy5_results.get('path'))
      
            cy3_channel_index = channel2index.get('Cy3')
            channels[cy3_channel_index].setMinDisplay(cy3_params.get('minDisplay'))
            channels[cy3_channel_index].setMaxDisplay(cy3_params.get('maxDisplay'))
            cy3_results = run_detection_for_window(server, channel2index, cy3_params, 'Cy3', current_scene_number, annot, image_display)
            print('Numb of cy3 detected :'+ cy3_results.get('detections_result').size())
      
            String file_name_cy3 = String.format("%s_annot_block_x%.0f_y%.0f_w%.0f_h%.0f", cy3_results.get('channel_name'),annot.getROI().getAllPoints()[0].getX(),annot.getROI().getAllPoints()[0].getY(),sub_image_width,sub_image_height)
            export_detector_cell_json(cy3_results.get('detections_result'), file_name_cy3, cy3_results.get('path'))
            
            
            removeObject(annot, false)
        }
        else {
            // spawn new annotation boxes from the current coordinate
            int sub_image_width = annot.getROI().x2 - annot.getROI().x
            int sub_image_height = annot.getROI().y2 - annot.getROI().y
            annotations =  add_annotation_boxes(sub_image_width, sub_image_height, plane, (int)annot.getROI().y, (int)annot.getROI().x, 0.5)
            removeObject(annot, false)
        }
    }
    if (counter >= 40){
        print('too much iteration already')
        break
    }
}
print('Done')
