#  Malayalam Sign Language Android App

This Android module is part of the **Malayalam Sign Language Recognition System**.  
It uses a trained **TensorFlow Lite model (`model.tflite`)** to recognize hand gestures corresponding to Malayalam alphabets and predict Malayalam words in real time.

---

##  Project Structure (Android Module)

- **`app/src/main/assets/`**
  - **`model.tflite`**  
    The TensorFlow Lite model used for gesture recognition.  
    🔹 If you update/retrain the model in the **Python module**, replace this file with the new one to use the updated model in the app.

  - **`malayalam_words.txt`**  
    A list of Malayalam words used for **word prediction**.  
    The app predicts words by sequentially combining recognized alphabets and checking against this dictionary.

  - **`1malayalam_words.txt`** *(optional test dictionary)*  
    Sample Malayalam words for testing different lexicons.

  - **`hand_landmarker.task`**  
    Mediapipe task file for detecting hand landmarks.

  - **`label_map.json`**  
    Maps gesture class indices to their corresponding Malayalam characters.

  - **`scaler_params.json`**  
    Preprocessing parameters (e.g., normalization values) used to make predictions consistent with the training environment.

- **`app/src/main/java/com/example/malayalamsignapp/`**
  - **`utils/`**  
    Contains helper classes for image processing, model validation, error handling, etc.
  - **`ui/`**  
    Contains UI components for rendering recognition output.

---

##  How It Works

1. **Gesture Recognition**  
   The app captures hand images using the device camera and extracts landmarks via Mediapipe.

2. **Alphabet Classification**  
   Landmarks are passed to the **`model.tflite`** file, which predicts the corresponding Malayalam alphabet.

3. **Word Prediction**  
   Recognized alphabets are concatenated, and the app matches them with words from **`malayalam_words.txt`** to form meaningful words.

4. **Output**  
   Predicted alphabets/words can be displayed on screen and optionally converted to **speech output** for accessibility.

---

##  Updating the Model

If you retrain the model in the Python project:

1. Copy the newly generated **`model.tflite`**.
2. Replace the existing one in:  
   ```
   app/src/main/assets/model.tflite
   ```
3. Rebuild the Android app.

---

##  Requirements

- Android Studio (latest recommended)
- Minimum SDK: **21+**
- TensorFlow Lite Support
- Mediapipe
- CameraX

---

##  Future Extensions

- Improve Prediction Accuracy
- Expand dictionary with more Malayalam words. 

---
