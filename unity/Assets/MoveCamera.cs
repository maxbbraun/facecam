using Braun.FaceCam;
using Google.Protobuf;
using System;
using UnityEngine;

public class MoveCamera : MonoBehaviour {
  public Vector3 position = new Vector3 (0.0f, 1.0f, -10.0f);
  public Quaternion rotation = Quaternion.identity;

  void Start() {
    transform.position = position;
    transform.rotation = rotation;
  }

  void Update() {
    transform.position = position;
    transform.rotation = rotation;
  }

  void Move(string message) {
    // Deserialize the proto.
    CameraPose pose = CameraPose.Parser.ParseFrom(Convert.FromBase64String(message));

    // Update the camera's position and rotation.
    position.Set(pose.PositionX, pose.PositionY, pose.PositionZ);
    rotation.Set(pose.RotationX, pose.RotationY, pose.RotationZ, pose.RotationW);
  }
}
