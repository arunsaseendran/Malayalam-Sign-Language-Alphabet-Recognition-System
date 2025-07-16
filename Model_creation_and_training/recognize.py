import cv2
import numpy as np
import mediapipe as mp
import json
from PIL import ImageFont, ImageDraw, Image
import tensorflow as tf

# === Load TFLite Model and Allocator ===
interpreter = tf.lite.Interpreter(model_path="model.tflite")
interpreter.allocate_tensors()

input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

# === Load scaler params from JSON ===
with open("scaler_params.json", "r") as f:
    scaler_params = json.load(f)
scaler_mean = np.array(scaler_params["mean"])
scaler_scale = np.array(scaler_params["scale"])

def scale_input(input_vector):
    return (np.array(input_vector) - scaler_mean) / scaler_scale

# === Load label map from JSON ===
with open("label_map.json", "r", encoding="utf-8") as f:
    label_map = json.load(f)
index_to_label = {int(k): v for k, v in label_map.items()}

# === Font for Malayalam Rendering ===
FONT_PATH = 'NotoSansMalayalam-VariableFont_wdth,wght.ttf'
def draw_malayalam_text(frame, text, position, font_size=48):
    image_pil = Image.fromarray(cv2.cvtColor(frame, cv2.COLOR_BGR2RGB))
    draw = ImageDraw.Draw(image_pil)
    try:
        font = ImageFont.truetype(FONT_PATH, font_size)
    except IOError:
        print(" Malayalam font not found. Place font in working directory.")
        return frame
    draw.text(position, text, font=font, fill=(0, 255, 0))
    return cv2.cvtColor(np.array(image_pil), cv2.COLOR_RGB2BGR)

# === MediaPipe Hands Setup ===
mp_hands = mp.solutions.hands
mp_drawing = mp.solutions.drawing_utils
hands = mp_hands.Hands(max_num_hands=2, min_detection_confidence=0.7, min_tracking_confidence=0.5)

# === Start Webcam ===
cap = cv2.VideoCapture(0)
print("📹 Starting real-time Malayalam sign recognition with TFLite... Press 'q' to exit.")

while True:
    ret, frame = cap.read()
    if not ret:
        break

    frame = cv2.flip(frame, 1)
    rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    result = hands.process(rgb)

    # Initialize feature vector: 126 features (21 points × 3 coords × 2 hands)
    features = [0.0] * 126
    hand_flags = {'Left': False, 'Right': False}

    if result.multi_hand_landmarks and result.multi_handedness:
        for idx, hand_landmark in enumerate(result.multi_hand_landmarks):
            hand_label = result.multi_handedness[idx].classification[0].label  # 'Left' or 'Right'
            hand_flags[hand_label] = True

            wrist = hand_landmark.landmark[0]
            coords = [
                (
                    lm.x - wrist.x,
                    lm.y - wrist.y,
                    lm.z - wrist.z
                ) for lm in hand_landmark.landmark
            ]

            start_idx = 0 if hand_label == 'Left' else 63  # Left hand: 0–62, Right hand: 63–125
            for i, (x, y, z) in enumerate(coords):
                features[start_idx + i] = x
                features[start_idx + 21 + i] = y
                features[start_idx + 42 + i] = z

            mp_drawing.draw_landmarks(frame, hand_landmark, mp_hands.HAND_CONNECTIONS)

    # Predict only if at least one hand is detected
    if hand_flags['Left'] or hand_flags['Right']:
        # Scale input features
        input_data = scale_input(features).astype(np.float32).reshape(1, -1)

        # Set tensor
        interpreter.set_tensor(input_details[0]['index'], input_data)

        # Run inference
        interpreter.invoke()

        # Get output and decode
        output_data = interpreter.get_tensor(output_details[0]['index'])
        predicted_index = np.argmax(output_data)
        label = index_to_label[predicted_index]

        frame = draw_malayalam_text(frame, f"അര്‍ത്ഥം: {label}", (10, 50))

    cv2.imshow("Malayalam Sign Recognition (TFLite)", frame)

    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

cap.release()
cv2.destroyAllWindows()
print(" Recognition session ended.")
