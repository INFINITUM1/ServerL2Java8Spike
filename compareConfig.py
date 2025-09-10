import os
import difflib

# папки для сравнения
folder1 = r"F:\Lineage2\pwsoftCompare\default"
folder2 = r"F:\Lineage2\pwsoftCompare\config"

# файл результата
result_file = "result.txt"

def is_comment_or_empty(line: str) -> bool:
    """Проверка, является ли строка комментарием или пустой"""
    stripped = line.strip()
    return stripped == "" or stripped.startswith("#") or stripped.startswith(";")

with open(result_file, "w", encoding="utf-8") as out:
    for root, _, files in os.walk(folder1):
        for file in files:
            file1_path = os.path.join(root, file)
            relative_path = os.path.relpath(file1_path, folder1)
            file2_path = os.path.join(folder2, relative_path)

            if os.path.exists(file2_path):
                with open(file1_path, encoding="utf-8", errors="ignore") as f1, \
                     open(file2_path, encoding="utf-8", errors="ignore") as f2:
                    text1 = f1.readlines()
                    text2 = f2.readlines()

                diff = list(difflib.ndiff(text1, text2))

                changes = []
                old_value = None

                for line in diff:
                    if line.startswith("- "):  # строка есть в старом
                        if not is_comment_or_empty(line[2:]):
                            old_value = line[2:].strip()
                    elif line.startswith("+ "):  # строка есть в новом
                        if not is_comment_or_empty(line[2:]):
                            new_value = line[2:].strip()
                            if old_value:  # совпала пара
                                changes.append((old_value, new_value))
                                old_value = None

                if changes:
                    out.write(f"\n=== {relative_path} ===\n")
                    for old, new in changes:
                        out.write(f"СТАРОЕ: {old}\n")
                        out.write(f"НОВОЕ: {new}\n")
                        out.write(f"MOBIUS:\n\n")

print(f"✅ Сравнение завершено! Результат в файле: {result_file}")
