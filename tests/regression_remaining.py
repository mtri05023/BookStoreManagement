"""Independent revenue fixture and the remaining console regression cases."""
import re
import shutil
import subprocess
from datetime import date, datetime, time, timedelta
from pathlib import Path


def rows(path):
    return path.read_text(encoding="utf-8").splitlines()


def customers(path):
    return [row for row in rows(path) if row.startswith("CUSTOMER|")]


def fixture(path, with_invoices):
    today = date.today()
    yesterday = today - timedelta(days=1)
    content = [
        "VERSION|4", "SETTING|LOW_STOCK|5",
        "BOOK|S001|Book One|Author A|100.0|5|Fiction|60.0",
        "BOOK|S002|Book Two|Author B|200.0|8|Science|120.0",
        "BOOK|S003|Book Three|Author C|50.0|7|History|30.0",
        "CUSTOMER|KH001|Alice|0900000001",
        "CUSTOMER|KH002|Bob|0900000002",
        "EMPLOYEE|NV001|Manager|Quản lý|0900000003",
        "SUPPLIER|NCC001|Alpha Supply|0900000004",
        "SUPPLIER|NCC002|Beta Supply|0900000005",
    ]
    if with_invoices:
        for invoice, customer, name, phone, day, hour, details in [
            ("HD001", "KH001", "Alice", "0900000001", yesterday, 9,
             [("S001", "Book One", 2, 100), ("S002", "Book Two", 1, 200)]),
            ("HD002", "KH002", "Bob", "0900000002", today, 10,
             [("S002", "Book Two", 2, 200), ("S003", "Book Three", 3, 50)]),
        ]:
            created = datetime.combine(day, time(hour, 0)).isoformat(timespec="minutes")
            paid = datetime.combine(day, time(hour, 5)).isoformat(timespec="minutes")
            content.append(f"INVOICE|{invoice}|{customer}|{name}|{phone}|NV001|Manager|Quản lý|{day}|{created}|{paid}|PAID")
            content.extend(f"DETAIL|{invoice}|{book}|{title}|{quantity}|{float(price)}"
                           for book, title, quantity, price in details)
    content += ["SOURCE|S001|NCC001|Alpha Supply", "SOURCE|S002|NCC002|Beta Supply",
                "SOURCE|S003|NCC001|Alpha Supply",
                f"END|3|2|1|{2 if with_invoices else 0}|{4 if with_invoices else 0}|3|2"]
    path.write_text("\n".join(content) + "\n", encoding="utf-8")
    return yesterday, today


def summary(section):
    match = re.search(r"Tổng doanh thu: ([\d,]+) VNĐ\s+Số hóa đơn: (\d+)\s+Số lượng sách bán ra: (\d+)\s+Số khách hàng: (\d+)", section)
    assert match, section[:500]
    return tuple(int(value.replace(",", "")) for value in match.groups())


def report_checks(work, build, run, java):
    data = work / "report-small.txt"
    yesterday, today = fixture(data, True)
    out = run(data, ["qaadmin", "admin123", "admin123", "qaadmin", "admin123",
                     "5", "3", "1", "1", "1", "1", "K", "HUY", "0", "0"], build)
    assert "Đã hủy hóa đơn" in out and not any(row.startswith("INVOICE|HD003|") for row in rows(data))
    baseline = rows(data)

    day1 = yesterday.strftime("%d/%m/%Y")
    day2 = today.strftime("%d/%m/%Y")
    choices = ["1", "2", day1, "3", str(today.month), str(today.year),
               "4", str(today.year), "5", day1, day2]
    for mode in range(6, 12):
        choices += [str(mode), "6"]
    out = run(data, ["qaadmin", "admin123", "8", *choices, "0", "0", "0"], build)
    sections = out.split("THỐNG KÊ DOANH THU:")[1:]
    assert len(sections) == 11, f"Opened {len(sections)} reports"
    print("PASS 11 revenue reports opened")

    sales = [(yesterday, 400, 1, 3, "KH001"), (today, 550, 1, 5, "KH002")]
    filters = [lambda d: d == today, lambda d: d == yesterday,
               lambda d: (d.month, d.year) == (today.month, today.year),
               lambda d: d.year == today.year,
               lambda d: yesterday <= d <= today]
    expected = []
    for predicate in filters:
        selected = [sale for sale in sales if predicate(sale[0])]
        expected.append((sum(s[1] for s in selected), sum(s[2] for s in selected),
                         sum(s[3] for s in selected), len({s[4] for s in selected})))
    expected += [(950, 2, 8, 2)] * 6
    for mode, (section, target) in enumerate(zip(sections, expected), 1):
        actual = summary(section)
        assert actual == target, f"Report {mode}: expected {target}, got {actual}"
    for mode in (6, 7, 8):
        section = sections[mode - 1]
        book_rows = re.findall(r"(?m)^(S00[1-3])\s+Book (?:One|Two|Three)\s+.*?\s+(\d+)\s+(\d+)\s*$", section)
        assert [(book, int(qty), int(revenue)) for book, qty, revenue in book_rows] == [
            ("S002", 3, 600), ("S003", 3, 150), ("S001", 2, 200)], f"Report {mode}: {book_rows}"
    assert "HD001 |" in sections[8] and "HD002 |" in sections[8]
    assert re.search(r"(?m)^KH002 \| 550\s*$", sections[9])
    assert re.search(r"(?m)^KH001 \| 400\s*$", sections[9])
    assert re.search(r"(?m)^1\s+Alpha Supply\s+5\s+350\s*$", sections[10])
    assert re.search(r"(?m)^2\s+Beta Supply\s+3\s+600\s*$", sections[10])
    assert rows(data) == baseline, "Read-only reports changed the fixture"
    print("PASS 11 revenue reports: amounts, quantities, order, customers and suppliers")
    return data


def removed_option_check(work, build, run, report_data):
    data = work / "removed-option-empty.txt"
    fixture(data, False)
    original = customers(data)
    out = run(data, ["qaadmin", "admin123", "admin123", "qaadmin", "admin123", "2", "14", "0", "0", "0"], build)
    assert "Lựa chọn không hợp lệ" in out and "Khởi tạo/thay danh sách" not in out
    assert customers(data) == original

    blocked = work / "removed-option-paid.txt"
    shutil.copy2(report_data, blocked)
    shutil.copy2(Path(str(report_data) + ".accounts"), Path(str(blocked) + ".accounts"))
    before = rows(blocked)
    out = run(blocked, ["qaadmin", "admin123", "2", "14", "0", "0", "0"], build)
    assert "Lựa chọn không hợp lệ" in out and "Khởi tạo/thay danh sách" not in out
    assert rows(blocked) == before
    print("PASS removed customer option 14 with and without invoices")


def boundary_checks(work, build, run):
    data = work / "boundaries.txt"
    fixture(data, False)
    out = run(data, ["qaadmin", "admin123", "admin123", "qaadmin", "admin123", "1",
                     "1", "0", "1234567890", "9", "Nine", "Author", "10", "20", "1",
                     "1", "S009",
                     "1", "123456789", "Max Nine", "Author", "10", "20", "1",
                     "0", "0", "0"], build)
    assert out.count("Chỉ nhập số nguyên dương") >= 2 and "Mã sách đã tồn tại" in out
    assert sum(row.startswith("BOOK|S009|") for row in rows(data)) == 1
    assert sum(row.startswith("BOOK|S123456789|") for row in rows(data)) == 1
    print("PASS code 0, 9 digits, 10 digits and normalized duplicate")

    out = run(data, ["qaadmin", "admin123", "5", "10", "1", "1", "1", "5", "K", "OK",
                     "5", "11", "1", "1", "2", "9", "2", "1", "K", "HUY",
                     "5", "12", "1", "1", "2", "5", "C", "2", "4", "3", "1", "K", "HUY",
                     "0", "0"], build)
    assert "THANH TOÁN THÀNH CÔNG" in out
    assert out.count("Không đủ tồn kho cho tổng số lượng") >= 2
    assert out.count("Đã hủy hóa đơn") == 2
    final = rows(data)
    assert any(row.startswith("BOOK|S001|") and "|100.0|0|" in row for row in final)
    assert any(row.startswith("BOOK|S002|") and "|200.0|8|" in row for row in final)
    assert any(row.startswith("BOOK|S003|") and "|50.0|7|" in row for row in final)
    assert sum(row.startswith("INVOICE|HD010|") for row in final) == 1
    assert not any(row.startswith("INVOICE|HD011|") or row.startswith("INVOICE|HD012|") for row in final)
    print("PASS exact stock, overstock and duplicate-book cart rejection with unchanged data")


def failure_check(work, build, java):
    path = work / "failure-store.txt"
    result = subprocess.run([str(java), "-cp", str(build), "FailureInjection", str(path)],
                            text=True, encoding="utf-8", capture_output=True, timeout=120)
    assert result.returncode == 0, result.stdout + result.stderr
    assert "PASS injected store failure" in result.stdout
    print("PASS injected store write failure and restart consistency")


def phone_search_check(work, build, run):
    data = work / "phone-search.txt"
    fixture(data, False)
    before = rows(data)
    out = run(data, ["qaadmin", "admin123", "admin123", "qaadmin", "admin123",
                     "2", "5", "09ab", "0900000001", "0",
                     "3", "5", "abc", "0900000003", "0", "0", "0"], build)
    assert out.count("Từ khóa số điện thoại chỉ được chứa chữ số") == 2
    assert "KH001" in out and "NV001" in out
    assert "Số khách hàng tìm thấy: 1" in out and "Số nhân viên tìm thấy: 1" in out
    assert rows(data) == before
    print("PASS customer and employee phone searches reject letters and retry")


def run_remaining(work, build, run, java):
    report_data = report_checks(work, build, run, java)
    removed_option_check(work, build, run, report_data)
    boundary_checks(work, build, run)
    failure_check(work, build, java)
    phone_search_check(work, build, run)
