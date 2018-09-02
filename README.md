# FaceCam

This Android app renders a 3D scene (made with Unity) using face tracking to move a virtual camera.

![screen recording](facecam.gif)

The [Android Studio](https://developer.android.com/studio/) and [Unity](https://unity3d.com/unity) projects can be imported from the [`android`](android) and [`unity`](unity) directories.

Basic flow:
 1. In [`FaceCamera.java`](android/src/main/java/fyi/braun/facecam/FaceCamera.java#L155), detect the 2D position and size of a face using the front-facing camera.
 2. In [`FaceCamera.java`](android/src/main/java/fyi/braun/facecam/FaceCamera.java#L99), use [OpenCV](https://www.learnopencv.com/head-pose-estimation-using-opencv-and-dlib/) to reconstruct the face position in 3D space.
 3. In [`UnityPlayerActivity.java`](https://github.com/maxbbraun/facecam/blob/master/android/src/main/java/fyi/braun/facecam/UnityPlayerActivity.java#L51), serialize the camera pose using [`camera_pose.proto`](android/src/main/proto/camera_pose.proto) and send it to the Unity scene.
 4. In [`MoveCamera.cs`](https://github.com/maxbbraun/facecam/blob/master/unity/Assets/MoveCamera.cs#L24), deserialize the pose and apply it to the scene's camera.

## License

Copyright 2018 Max Braun

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
