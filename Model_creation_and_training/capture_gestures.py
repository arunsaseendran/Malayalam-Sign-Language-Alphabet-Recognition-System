import cv2
import mediapipe as mp
import numpy as np
import csv
import os
import time
from PIL import Image, ImageDraw, ImageFont
import pandas as pd

malayalam_alphabets = [

    'അ', 'ആ', 'ഇ', 'ഈ', 'ഉ', 'ഊ', 'ഋ', 'എ', 'ഏ', 'ഐ', 'ഒ', 'ഓ', 'ഔ', 'അം', 'അഃ',
    

    'ക', 'ഖ', 'ഗ', 'ഘ', 'ങ',
    'ച', 'ഛ', 'ജ', 'ഝ', 'ഞ', 
    'ട', 'ഠ', 'ഡ', 'ഢ', 'ണ',
    'ത', 'ഥ', 'ദ', 'ധ', 'ന',
    'പ', 'ഫ', 'ബ', 'ഭ', 'മ',
    'യ', 'ര', 'ല', 'വ',
    'ശ', 'ഷ', 'സ', 'ഹ',
    'ള', 'ഴ', 'റ',
    

    'ൺ', 'ൻ', 'ർ', 'ൽ', 'ൾ'
    'ാ','ി','ീ','ു','ൂ','ൃ','ൄ','െ','ൈ','േ','ൗ'
]

print(f"Total Malayalam alphabets to capture: {len(malayalam_alphabets)}")
print("Alphabets:", malayalam_alphabets)








# ========== PIL-based Malayalam Text Renderer ==========
def draw_malayalam_text(frame, text, position, font_path='NotoSansMalayalam-VariableFont_wdth,wght.ttf', font_size=32):
    image_pil = Image.fromarray(cv2.cvtColor(frame, cv2.COLOR_BGR2RGB))
    draw = ImageDraw.Draw(image_pil)
    try:
        font = ImageFont.truetype(font_path, font_size)
    except IOError:
        print("⚠️ Font file not found. Please place 'NotoSansMalayalam-Regular.ttf' in the working directory.")
        return frame
    draw.text(position, text, font=font, fill=(0, 255, 0))
    return cv2.cvtColor(np.array(image_pil), cv2.COLOR_RGB2BGR)

# ========== Helper Functions ==========
def get_captured_letters(csv_filename):
    """Get list of letters that have been captured in CSV"""
    if not os.path.exists(csv_filename):
        return []
    
    try:
        df = pd.read_csv(csv_filename, encoding='utf-8')
        captured = df['label'].unique().tolist()
        return captured
    except:
        return []

def get_letters_to_capture(all_letters, captured_letters):
    """Get list of letters that still need to be captured"""
    return [letter for letter in all_letters if letter not in captured_letters]

def remove_letter_data(csv_filename, letter):
    """Remove all data for a specific letter from CSV"""
    if not os.path.exists(csv_filename):
        return
    
    try:
        df = pd.read_csv(csv_filename, encoding='utf-8')
        df = df[df['label'] != letter]
        df.to_csv(csv_filename, index=False, encoding='utf-8')
        print(f" Removed existing data for '{letter}' from CSV")
    except Exception as e:
        print(f" Error removing data: {e}")

def get_next_letter_to_capture(all_letters, csv_filename):
    """Get the next letter to capture based on what's already in CSV"""
    captured_letters = get_captured_letters(csv_filename)
    
    for letter in all_letters:
        if letter not in captured_letters:
            return letter
    
    return None  # All letters captured

def remove_letter_images(image_folder, letter):
    """Remove all images for a specific letter"""
    gesture_folder = os.path.join(image_folder, letter)
    if os.path.exists(gesture_folder):
        import shutil
        shutil.rmtree(gesture_folder)
        print(f" Removed existing images for '{letter}'")

# ========== Create/Open CSV ==========
# csv_filename2 = "Consonants.csv"
csv_filename = "data_both_hands.csv"
image_folder = "malayalam_isl_images"
if not os.path.exists(image_folder):
    os.makedirs(image_folder)

def initialize_csv():
    """Initialize CSV with headers if it doesn't exist"""
    if not os.path.exists(csv_filename):
        with open(csv_filename, mode='w', newline='', encoding='utf-8') as f:
            writer = csv.writer(f)
            header = ['label', 'image_path', 'is_left', 'is_right']
            for hand_prefix in ['L', 'R']:
                header += [f'{hand_prefix}_x{i}' for i in range(21)]
                header += [f'{hand_prefix}_y{i}' for i in range(21)]
                header += [f'{hand_prefix}_z{i}' for i in range(21)]
            writer.writerow(header)

    # if not os.path.exists(csv_filename2):
    #     with open(csv_filename2, mode='w', newline='', encoding='utf-8') as f:
    #         writer = csv.writer(f)
    #         header = ['label', 'image_path', 'is_left', 'is_right']
    #         for hand_prefix in ['L', 'R']:
    #             header += [f'{hand_prefix}_x{i}' for i in range(21)]
    #             header += [f'{hand_prefix}_y{i}' for i in range(21)]
    #             header += [f'{hand_prefix}_z{i}' for i in range(21)]
    #         writer.writerow(header)

initialize_csv()

# ========== MediaPipe Setup ==========
mp_hands = mp.solutions.hands
mp_drawing = mp.solutions.drawing_utils
hands = mp_hands.Hands(max_num_hands=2, min_detection_confidence=0.7, min_tracking_confidence=0.5)

# ========== Video Capture ==========
cap = cv2.VideoCapture(0)

def capture_letter_data(label):
    """Capture data for a specific letter"""
    print(f"\n Preparing to capture '{label}'... Get ready!")
    gesture_folder = os.path.join(image_folder, label)
    if not os.path.exists(gesture_folder):
        os.makedirs(gesture_folder)

    # Countdown
    for countdown in range(3, 0, -1):
        ret, frame = cap.read()
        if not ret:
            break
        frame = cv2.flip(frame, 1)
        frame = draw_malayalam_text(frame, f"Prepare: {countdown}", (150, 250), font_size=48)
        frame = draw_malayalam_text(frame, f"Letter: {label}", (150, 150), font_size=64)
        cv2.imshow("Malayalam ISL Data Collection", frame)
        cv2.waitKey(1000)

    print(f"\n Collecting data for '{label}' (10 sec, 100 images)...")
    frame_count = 0
    start_time = time.time()

    while frame_count < 100:
        ret, frame = cap.read()
        if not ret:
            break

        frame = cv2.flip(frame, 1)
        rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        result = hands.process(rgb_frame)

        elapsed_time = time.time() - start_time
        target_frame = int(elapsed_time * 10)

        if frame_count < target_frame:
            coords = {'Left': {'x': [0]*21, 'y': [0]*21, 'z': [0]*21},
                      'Right': {'x': [0]*21, 'y': [0]*21, 'z': [0]*21}}
            is_left, is_right = 0, 0

            if result.multi_hand_landmarks and result.multi_handedness:
                for idx, hand_landmark in enumerate(result.multi_hand_landmarks):
                    hand_label = result.multi_handedness[idx].classification[0].label  # 'Left' or 'Right'
                    if hand_label == 'Left': is_left = 1
                    if hand_label == 'Right': is_right = 1

                    landmarks = hand_landmark.landmark
                    wrist = landmarks[0]

                    coords[hand_label]['x'] = [lm.x - wrist.x for lm in landmarks]
                    coords[hand_label]['y'] = [lm.y - wrist.y for lm in landmarks]
                    coords[hand_label]['z'] = [lm.z - wrist.z for lm in landmarks]

                    mp_drawing.draw_landmarks(frame, hand_landmark, mp_hands.HAND_CONNECTIONS)

            # Save image
            image_filename = f"{label}_{frame_count}.jpg"
            image_path = os.path.join(gesture_folder, image_filename)
            is_success, buffer = cv2.imencode(".jpg", frame)
            if is_success:
                with open(image_path, "wb") as f:
                    f.write(buffer)

            # Save to CSV
            with open(csv_filename, mode='a', newline='', encoding='utf-8') as f:
                writer = csv.writer(f)
                row = [label, image_path, is_left, is_right]
                for hand in ['Left', 'Right']:
                    for axis in ['x', 'y', 'z']:
                        row += coords[hand][axis]
                writer.writerow(row)

            # with open(csv_filename2, mode='a', newline='', encoding='utf-8') as f:
            #     writer = csv.writer(f)
            #     row = [label, image_path, is_left, is_right]
            #     for hand in ['Left', 'Right']:
            #         for axis in ['x', 'y', 'z']:
            #             row += coords[hand][axis]
            #     writer.writerow(row)

            frame_count += 1

        # Display progress
        frame = draw_malayalam_text(frame, f"Letter : {label} | Captured : {frame_count}/100", (10, 50))
        progress = int((frame_count / 100) * 400)
        cv2.rectangle(frame, (10, 100), (410, 120), (50, 50, 50), -1)
        cv2.rectangle(frame, (10, 100), (10 + progress, 120), (0, 255, 0), -1)
        
        cv2.imshow("Malayalam ISL Data Collection", frame)

        if cv2.waitKey(1) & 0xFF == ord('q'):
            print(" Capture interrupted by user")
            return False

    print(f" Data collection complete for '{label}'")
    return True

print("\n Malayalam ISL Data Collection")
print("=" * 50)

while True:
    # Get current status
    captured_letters = get_captured_letters(csv_filename)
    letters_to_capture = get_letters_to_capture(malayalam_alphabets, captured_letters)
    
    print(f"\n Status:")
    print(f"   Captured: {len(captured_letters)}/{len(malayalam_alphabets)} letters")
    print(f"   Remaining: {len(letters_to_capture)} letters")
    
    print(f"\nChoose an option:")
    print("1. Capture specific alphabet")
    print("2. Capture alphabets continuously") 
    print("3. Captured letters")
    print("4. Letters to be captured")
    print("5. Exit")
    
    choice = input("Enter your choice (1-5): ").strip()
    
    if choice == '5':
        break
    
    elif choice == '1':
        # Capture specific alphabet with sub-menu
        while True:
            print(f"\n Capture Specific Alphabet:")
            # print("1. Enter alphabet")
            # print("2. Exit to main menu")
            
            # sub_choice = input("Enter your choice (1-2): ").strip()
            
            # if sub_choice == '2':
            #     break
            # elif sub_choice == '1':
            label = input("\nEnter Malayalam alphabet: ").strip()
            # if label not in malayalam_alphabets:
            #     print(f" '{label}' is not in the available list!")
            #     continue
            
            # Remove existing data for this letter
            remove_letter_data(csv_filename, label)
            remove_letter_images(image_folder, label)
            
            # remove_letter_data(csv_filename2, label)

            # Capture new data
            success = capture_letter_data(label)
            if success:
                print(f" Successfully captured data for '{label}'")
            else:
                print(f" Data capture incomplete for '{label}'")
        else:
            print(" Invalid choice!")
    
    elif choice == '2':
        # Capture alphabets continuously
        if not letters_to_capture:
            print(" All letters have been captured!")
            continue
            
        print(f"\n Continuous capture mode")
        print(f"Will capture {len(letters_to_capture)} remaining letters:")
        print(letters_to_capture[:10], "..." if len(letters_to_capture) > 10 else "")
        
        confirm = input("\nContinue? (y/n): ").strip().lower()
        if confirm != 'y':
            continue
            
        for i, label in enumerate(letters_to_capture):
            print(f"\n Progress: {i+1}/{len(letters_to_capture)}")
            success = capture_letter_data(label)
            
            if not success:
                print(" Continuous capture interrupted")
                break
                
            # Short break between letters
            if i < len(letters_to_capture) - 1:
                print(" 5 second break before next letter...")
                for j in range(5, 0, -1):
                    ret, frame = cap.read()
                    if ret:
                        frame = cv2.flip(frame, 1)
                        next_letter = letters_to_capture[i+1]
                        frame = draw_malayalam_text(frame, f"Next Letter {next_letter}", (100, 150), font_size=32)
                        frame = draw_malayalam_text(frame, f"{j} Seconds", (100, 200), font_size=32)
                        cv2.imshow("Malayalam ISL Data Collection", frame)
                    cv2.waitKey(1000)
        
        print(" Continuous capture completed!")
    
    elif choice == '3':
        # Show captured letters
        if captured_letters:
            print(f"\n Captured Letters ({len(captured_letters)}):")
            for i, letter in enumerate(captured_letters, 1):
                print(f"{i:2d}. {letter}")
        else:
            print("\n No letters captured yet!")
    
    elif choice == '4':
        # Show letters to be captured
        if letters_to_capture:
            print(f"\n Letters to be captured ({len(letters_to_capture)}):")
            for i, letter in enumerate(letters_to_capture, 1):
                print(f"{i:2d}. {letter}")
        else:
            print("\n All letters have been captured!")
    
    else:
        print(" Invalid choice!")

# Cleanup
cap.release()
cv2.destroyAllWindows()
print("\n Malayalam ISL Data Collection Session Ended!")
print(f" Images saved in: {image_folder}")
print(f" Data saved in: {csv_filename}")

# Final summary
final_captured = get_captured_letters(csv_filename)
print(f"\n Final Summary:")
print(f"   Total captured: {len(final_captured)}/{len(malayalam_alphabets)} letters")
if len(final_captured) == len(malayalam_alphabets):
    print(" Congratulations! All Malayalam letters have been captured!")
else:
    remaining = len(malayalam_alphabets) - len(final_captured)
    print(f"   Remaining: {remaining} letters to capture")