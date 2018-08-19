using System.Collections;
using System.Text.RegularExpressions;
using UnityEngine;

public class MoveCamera : MonoBehaviour {
  private const string posePattern = @"P (-?\d+\.\d+) (-?\d+\.\d+) (-?\d+\.\d+) R (-?\d+\.\d+) (-?\d+\.\d+) (-?\d+\.\d+) (-?\d+\.\d+)";

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
    Match poseMatch = Regex.Match(message, posePattern);
    if (poseMatch.Success) {
      position.Set(
        float.Parse(poseMatch.Groups[1].Value),
        float.Parse(poseMatch.Groups[2].Value),
        float.Parse(poseMatch.Groups[3].Value));
      rotation.Set(
        float.Parse(poseMatch.Groups[4].Value),
        float.Parse(poseMatch.Groups[5].Value),
        float.Parse(poseMatch.Groups[6].Value),
        float.Parse(poseMatch.Groups[7].Value));
    } else {
      Debug.LogError("Unknown message: " + message);
    }
  }
}
