import pandas as pd
import numpy as np
from sklearn.preprocessing import StandardScaler
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, confusion_matrix
import tensorflow as tf
import json
import seaborn as sns
import matplotlib.pyplot as plt
import joblib

# === Define Malayalam alphabets in ISL order ===
malayalam_alphabets = [
    # Vowels
    'അ', 'ആ', 'ഇ', 'ഈ', 'ഉ', 'ഊ', 'ഋ', 'എ', 'ഏ', 'ഐ', 'ഒ', 'ഓ', 'ഔ', 'അം', 'അഃ',
    
    # Consonants
    'ക', 'ഖ', 'ഘ', 'ഗ', 'ങ',
    'ച', 'ഛ', 'ജ', 'ഝ', 'ഞ', 
    'ട', 'ഠ', 'ഡ', 'ഢ', 'ണ',
    'ത', 'ഥ', 'ദ', 'ധ', 'ന',
    'പ', 'ഫ', 'ബ', 'ഭ', 'മ',
    'യ', 'ര', 'ല', 'വ',
    'ശ', 'ഷ', 'സ', 'ഹ',
    'ള', 'ഴ', 'റ',
    
    # Additional characters
    'ൺ', 'ൻ', 'ർ', 'ൽ', 'ൾ'
]

print(f"Total expected alphabets: {len(malayalam_alphabets)}")

# === Custom Label Encoder that preserves ISL order ===
class ISLLabelEncoder:
    def __init__(self, classes):
        self.classes_ = classes
        self.class_to_index = {cls: idx for idx, cls in enumerate(classes)}
    
    def fit_transform(self, y):
        return np.array([self.class_to_index[label] for label in y])
    
    def transform(self, y):
        return np.array([self.class_to_index[label] for label in y])
    
    def inverse_transform(self, y_encoded):
        return np.array([self.classes_[idx] for idx in y_encoded])

# === Load CSV Data ===
csv_path = "data_both_hands.csv"
try:
    df = pd.read_csv(csv_path)
    print(f" Loaded {len(df)} rows from {csv_path}")
except FileNotFoundError:
    print(f" File {csv_path} not found!")
    print("Please run the capture script first to collect data.")
    exit()

print(f"\n Data Summary:")
print(f"Total rows: {len(df)}")
print(f"Unique labels in dataset: {len(df['label'].unique())}")
print(f"Labels in data: {sorted(df['label'].unique())}")

# === Data Quality Check ===
print(f"\n Data Quality Check:")
label_counts = df['label'].value_counts()
print("Samples per label:")
for label, count in label_counts.items():
    print(f"  {label}: {count} samples")

# Check for labels with too few samples
min_samples = 50  # Minimum samples recommended per class
low_sample_labels = label_counts[label_counts < min_samples]
if len(low_sample_labels) > 0:
    print(f"\n Labels with fewer than {min_samples} samples:")
    for label, count in low_sample_labels.items():
        print(f"  {label}: {count} samples")

# === Clean Data ===
# Only keep rows where at least one hand is detected
df_clean = df[(df['is_left'] == 1) | (df['is_right'] == 1)].reset_index(drop=True)
print(f"\n After cleaning (hand detected): {len(df_clean)} rows")

# === Filter available labels and maintain ISL order ===
available_labels = df_clean['label'].unique()
filtered_malayalam_alphabets = [label for label in malayalam_alphabets if label in available_labels]
missing_labels = [label for label in malayalam_alphabets if label not in available_labels]

print(f"\n Available for training: {len(filtered_malayalam_alphabets)} labels")
print(f" Training labels: {filtered_malayalam_alphabets}")

if missing_labels:
    print(f"\n Missing labels (need to collect data): {len(missing_labels)}")
    print(f" Missing: {missing_labels}")

# Filter dataframe to only include available labels
df_final = df_clean[df_clean['label'].isin(filtered_malayalam_alphabets)].reset_index(drop=True)
print(f"\n Final dataset: {len(df_final)} rows")

# === Extract features ===
left_features = [f'L_{axis}{i}' for axis in ['x', 'y', 'z'] for i in range(21)]
right_features = [f'R_{axis}{i}' for axis in ['x', 'y', 'z'] for i in range(21)]
feature_columns = left_features + right_features

# Ensure all feature columns exist
for col in feature_columns:
    if col not in df_final.columns:
        df_final[col] = 0.0

X = df_final[feature_columns].fillna(0).values
y = df_final["label"].values

print(f"\n Feature extraction:")
print(f"Feature shape: {X.shape}")
print(f"Target shape: {y.shape}")

# === Encode labels with ISL order ===
label_encoder = ISLLabelEncoder(filtered_malayalam_alphabets)
y_encoded = label_encoder.fit_transform(y)

print(f"\n Label encoding:")
print(f"Number of classes: {len(label_encoder.classes_)}")
print("Label mapping:")
for i, label in enumerate(label_encoder.classes_):
    count = np.sum(y_encoded == i)
    print(f"  {i:2d}: {label} ({count} samples)")

# === Normalize features ===
scaler = StandardScaler()
X_scaled = scaler.fit_transform(X)

# === Split data for proper evaluation ===
X_train, X_test, y_train, y_test = train_test_split(
    X_scaled, y_encoded, test_size=0.2, random_state=42, stratify=y_encoded
)
# === Save test data for future evaluation ===
# np.save("X_test_rf.npy", X_test)
# np.save("y_test_rf.npy", y_test)


print(f"\n Data split:")
print(f"Training: {X_train.shape[0]} samples")
print(f"Testing: {X_test.shape[0]} samples")

# === Train RandomForest ===
print(f"\n Training RandomForest...")
rf_model = RandomForestClassifier(n_estimators=100, random_state=42)
rf_model.fit(X_train, y_train)

# Save the trained Random Forest model
joblib.dump(rf_model, "random_forest_model.pkl")
print("✅ Random Forest model saved as random_forest_model.pkl")

rf_train_accuracy = rf_model.score(X_train, y_train)
rf_test_accuracy = rf_model.score(X_test, y_test)

print(f"RandomForest Training Accuracy: {rf_train_accuracy:.4f}")
print(f"RandomForest Test Accuracy: {rf_test_accuracy:.4f}")






# === Train TensorFlow model ===
print(f"\n Training Neural Network...")
input_shape = X_scaled.shape[1]  # Should be 126
num_classes = len(label_encoder.classes_)

tf_model = tf.keras.Sequential([
    tf.keras.layers.Input(shape=(input_shape,)),
    tf.keras.layers.Dense(256, activation='relu'),
    tf.keras.layers.BatchNormalization(),
    tf.keras.layers.Dropout(0.3),
    tf.keras.layers.Dense(128, activation='relu'),
    tf.keras.layers.BatchNormalization(),
    tf.keras.layers.Dropout(0.2),
    tf.keras.layers.Dense(64, activation='relu'),
    tf.keras.layers.Dropout(0.1),
    tf.keras.layers.Dense(num_classes, activation='softmax')
])

tf_model.compile(
    optimizer=tf.keras.optimizers.Adam(learning_rate=0.001),
    loss='sparse_categorical_crossentropy',
    metrics=['accuracy']
)

# Train with validation
history = tf_model.fit(
    X_train, y_train,
    validation_data=(X_test, y_test),
    epochs=100,
    batch_size=32,
    verbose=1,
    callbacks=[
        tf.keras.callbacks.EarlyStopping(patience=15, restore_best_weights=True),
        tf.keras.callbacks.ReduceLROnPlateau(patience=10, factor=0.5)
    ]
)

# === Evaluate model ===
test_loss, test_accuracy = tf_model.evaluate(X_test, y_test, verbose=0)
print(f"\n Neural Network Test Accuracy: {test_accuracy:.4f}")

# === Convert to TensorFlow Lite ===
print(f"\n Converting to TensorFlow Lite...")
converter = tf.lite.TFLiteConverter.from_keras_model(tf_model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_model = converter.convert()

with open("model.tflite", "wb") as f:
    f.write(tflite_model)

# === Save all necessary files ===
print(f"\n Saving model files...")

# Save scaler parameters
scaler_params = {
    "mean": scaler.mean_.tolist(),
    "scale": scaler.scale_.tolist()
}
with open("scaler_params.json", "w") as f:
    json.dump(scaler_params, f)

# Save label map
label_map = {int(i): label for i, label in enumerate(label_encoder.classes_)}
with open("label_map.json", "w", encoding="utf-8") as f:
    json.dump(label_map, f, ensure_ascii=False, indent=2)

# Save comprehensive info
model_info = {
    "total_malayalam_alphabets": len(malayalam_alphabets),
    "trained_alphabets": len(filtered_malayalam_alphabets),
    "complete_alphabet_list": malayalam_alphabets,
    "trained_labels": filtered_malayalam_alphabets,
    "missing_labels": missing_labels,
    "model_accuracy": float(test_accuracy),
    "input_features": len(feature_columns),
    "feature_order": feature_columns,
    "training_samples": int(len(X_train)),
    "test_samples": int(len(X_test))
}
with open("malayalam_isl_info.json", "w", encoding="utf-8") as f:
    json.dump(model_info, f, ensure_ascii=False, indent=2)

# === Test TFLite model ===
print(f"\n Testing TFLite model...")
interpreter = tf.lite.Interpreter(model_path="model.tflite")
interpreter.allocate_tensors()

input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

print(f"TFLite input shape: {input_details[0]['shape']}")
print(f"TFLite output shape: {output_details[0]['shape']}")

# Test with a few samples
for i in range(min(5, len(X_test))):
    test_input = X_test[i:i+1].astype(np.float32)
    interpreter.set_tensor(input_details[0]['index'], test_input)
    interpreter.invoke()
    output_data = interpreter.get_tensor(output_details[0]['index'])
    
    predicted_class = np.argmax(output_data[0])
    confidence = np.max(output_data[0])
    actual_label = label_encoder.classes_[y_test[i]]
    predicted_label = label_encoder.classes_[predicted_class]
    
    status = "" if predicted_label == actual_label else "XXX"
    print(f"{status} Actual: {actual_label} | Predicted: {predicted_label} | Confidence: {confidence:.3f}")

print(f"\n Model training complete!")
print(f" Generated files:")
print(f"  - model.tflite (TensorFlow Lite model)")
print(f"  - scaler_map.json (Feature scaling parameters)")
print(f"  - label_map.json (Label mappings)")
print(f"  - malayalam_isl_info.json (Complete model information)")
print(f"\n Model Statistics:")
print(f"  - Input features: {input_shape}")
print(f"  - Output classes: {num_classes}")
print(f"  - Test accuracy: {test_accuracy:.4f}")
print(f"  - Trained on {len(filtered_malayalam_alphabets)} Malayalam letters")

# if missing_labels:
#     print(f"\n To improve coverage, collect data for these missing letters:")
#     for i, label in enumerate(missing_labels, 1):
#         print(f"  {i}. {label}")
