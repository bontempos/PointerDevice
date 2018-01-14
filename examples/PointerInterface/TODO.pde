/*

  CALIBRATION:
  currently when entering calibration mode, library auto detects mouse movement so no need to send information from the app.
  however this is done "manually" on mouseFollow().
  
  GRAPHIC INTERFACE:
  draw a representation of homography points inside servo canvas
  
  make a square shape where hophography points are inside.
  this square can be scaled with or without affecting its inside points.
  
  these squares can be placed on the main canvas. Each pointers has its onw object.
  
  create an option to draw planes on the canvas representing the projetcion plane
  these objects must be squared (representing servo ranges) but they can be masked
  on the main canvas
  
  for now these objects are going to be planar in relation to main canvas/view

*/