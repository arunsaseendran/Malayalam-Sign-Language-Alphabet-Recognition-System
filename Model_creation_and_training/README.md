# This Part Contains Gesture Capturing and Model Creation
---

##  Project Structure

- **`capture_gestures.py`**  
  Used to create gesture samples. Captured gestures are saved into the folder **`malayalam_isl_images/`** and stored as features in a CSV file.

- **`data_both_hands.csv`**  
  A dataset file containing the collected gesture samples (features extracted from images).

- **`train_models.py`**  
  Script for training the gesture recognition model using the dataset (`data_both_hands.csv`). Trained models are exported for later use.

- **`model.tflite`**  
  A TensorFlow Lite model file generated after training. Optimized for lightweight and real-time gesture recognition.

- **`recognize.py`**  
  Used to test and recognize gestures using the trained model (`model.tflite`). This script evaluates real-time input gestures against the trained dataset.

- **`requirements.txt`**  
  Lists all Python dependencies required to run the project.

- **Other Files**:
  - **`malayalam_isl_images/`** – Folder containing captured gesture images.
  - **`random_forest_model.pkl`** – Saved Scikit-learn Random Forest model (alternative model).
  - **`scaler_params.json`** – Contains scaler parameters used for preprocessing.
  - **`label_map.json` / `malayalam_isl_info.json`** – Mapping between gestures and class labels.

---

##  How to Run

### 1. Install Dependencies
```bash
pip install -r requirements.txt
```

### 2. Run Gesture Recognition
```bash
python recognize.py
```
This script loads the pre-trained model (`model.tflite`) and tests gestures in real-time.

### 🔹 If You Want to Add New Gestures and Retrain
1. **Capture Gesture Samples**
   ```bash
   python capture_gestures.py
   ```
   This will save gesture images into `malayalam_isl_images/` and update `data_both_hands.csv`.

2. **Train the Model**
   ```bash
   python train_models.py
   ```
   This generates `model.tflite` and other model files.

---

##  Requirements
Main libraries used:
- TensorFlow / TensorFlow Lite
- OpenCV
- Mediapipe
- NumPy, Pandas
- Scikit-learn
- Matplotlib, Seaborn

Install all dependencies from `requirements.txt` before running.

---

##  Notes
- Ensure your webcam is connected for gesture capture and recognition.
- For better performance, retrain the model with more samples.
- The project can be extended for **real-time ISL translation** applications.

---

##  Future Improvements
- Add support for more gestures.
- Improve accuracy with deep learning models (CNNs).
- Develop a UI for easier testing and interaction.

