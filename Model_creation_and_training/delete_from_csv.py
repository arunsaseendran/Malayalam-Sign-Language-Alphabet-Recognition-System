
# -----------------------------------This code only deletes rows in CSV file delete images manually---------------------------------------


import csv

# File path
csv_filename = "data_both_hands.csv"

# Ask for the label to delete
label_to_delete = input("Enter the Malayalam alphabet to delete: ").strip()

# Read all rows from the CSV
with open(csv_filename, mode='r', encoding='utf-8') as f:
    reader = csv.reader(f)
    rows = list(reader)

# Keep header and filter out rows with the specified label
header = rows[0]
filtered_rows = [row for row in rows[1:] if row[0] != label_to_delete]

# Write the updated data back to CSV
with open(csv_filename, mode='w', newline='', encoding='utf-8') as f:
    writer = csv.writer(f)
    writer.writerow(header)
    writer.writerows(filtered_rows)

print(f" Deleted all entries with label '{label_to_delete}' from {csv_filename}")
