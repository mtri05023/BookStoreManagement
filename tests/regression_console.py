"""Console regression checks. Runs only against disposable copies of data/store.txt."""
import os
import shutil
import subprocess
import tempfile
from datetime import date
from pathlib import Path
from regression_remaining import run_remaining

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "data" / "store.txt"
JDK = Path(os.environ.get("JAVA_HOME", r"C:\Program Files\Java\jdk-27")) / "bin"
JAVA = JDK / "java.exe"
JAVAC = JDK / "javac.exe"


def run(data, steps, build):
    result = subprocess.run(
        [str(JAVA), "-cp", str(build), "presentation.Main", "--data", str(data)],
        input="\n".join(steps) + "\n", text=True, encoding="utf-8",
        capture_output=True, timeout=120, cwd=ROOT,
    )
    assert result.returncode == 0, result.stderr + result.stdout[-1200:]
    assert not result.stderr, result.stderr
    return result.stdout


def lines(path):
    return path.read_text(encoding="utf-8").splitlines()


def main():
    assert SOURCE.resolve().is_relative_to(ROOT.resolve())
    with tempfile.TemporaryDirectory(prefix="regression-", dir=ROOT / "data") as folder:
        work = Path(folder).resolve()
        assert work.is_relative_to((ROOT / "data").resolve())
        build = work / "classes"
        build.mkdir()
        sources = sorted(str(p) for p in (ROOT / "src").rglob("*.java"))
        sources.append(str(ROOT / "tests" / "FailureInjection.java"))
        subprocess.run([str(JAVAC), "--release", "17", "-encoding", "UTF-8", "-d", str(build), *sources], check=True)

        data = work / "store.txt"
        shutil.copy2(SOURCE, data)
        out = run(data, ["qaadmin", "admin123", "admin123", "qaadmin", "admin123",
                         "4", "2", "990", "QA Supplier", "091234567", "09123456789",
                         "1912345678", "09123a5678", "0912345678", "0", "0", "0"], build)
        assert out.count("Số điện thoại phải có đúng 10 chữ số") == 4
        assert out.count("Thêm nhà cung cấp thành công") == 1
        assert sum(row.startswith("SUPPLIER|NCC990|") for row in lines(data)) == 1
        print("PASS supplier phone retries")

        out = run(data, ["qaadmin", "admin123", "2",
                         "1", "990", "An QA One", "0901112222",
                         "1", "991", "Lan An QA", "0901113333",
                         "1", "992", "Duplicate QA", "0901112222",
                         "6", "990", "1", "An QA Updated",
                         "6", "990", "2", "0901113333", "0901114444", "0",
                         "3", "1", "990", "Employee QA", "0902223333", "2", "0",
                         "9", "2", "990", "qaemployee", "employee123", "employee123", "0", "0",
                         "qaemployee", "employee123", "2", "3", "An", "2", "990", "0", "0",
                         "qaadmin", "admin123", "3", "8", "990", "0", "9", "1", "0", "0",
                         "qaemployee", "employee123", "0"], build)
        assert "Số điện thoại khách hàng đã tồn tại" in out
        assert "Số điện thoại mới: Số điện thoại khách hàng đã tồn tại" in out
        assert "Đăng nhập thành công. Xin chào qaemployee" in out
        employee_search = out.split("Đăng nhập thành công. Xin chào qaemployee", 1)[1].split("Đã đăng xuất.", 1)[0]
        assert "Tên hoặc từ khóa: " in employee_search and "KH990" in employee_search and "KH991" in employee_search
        assert "Xóa nhân viên và tài khoản liên kết thành công" in out
        assert out.count("Tên đăng nhập hoặc mật khẩu không đúng") == 1
        rows = lines(data)
        assert "CUSTOMER|KH990|An QA Updated|0901114444" in rows
        assert "CUSTOMER|KH991|Lan An QA|0901113333" in rows
        assert not any(row.startswith("CUSTOMER|KH992|") or row.startswith("EMPLOYEE|NV990|") for row in rows)
        assert not any("qaemployee" in row for row in lines(Path(str(data) + ".accounts")))
        out = run(data, ["qaemployee", "employee123", "0"], build)
        assert "Tên đăng nhập hoặc mật khẩu không đúng" in out
        print("PASS customer uniqueness, employee name search, linked account deletion and restart")

        orphan = work / "orphan.txt"
        shutil.copy2(data, orphan)
        account_rows = lines(Path(str(data) + ".accounts"))
        admin = next(row for row in account_rows if row.startswith("ACCOUNT|A001|"))
        password_hash = admin.split("|")[3]
        Path(str(orphan) + ".accounts").write_text(
            "ACCOUNTS|1\n" + admin + "\n" +
            f"ACCOUNT|A002|orphanqa|{password_hash}|EMPLOYEE|NV999|true|false\nEND|2\n", encoding="utf-8")
        out = run(orphan, ["orphanqa", "admin123", "0"], build)
        assert "Tên đăng nhập hoặc mật khẩu không đúng" in out
        print("PASS orphan employee account rejected")

        out = run(data, ["qaadmin", "admin123", "1",
                         "1", "990", "QA Book", "QA Author", "100", "200", "2",
                         "5", "990", "1", "QA Book Edited",
                         "5", "990", "5", "300",
                         "5", "990", "0",
                         "5", "990", "6", "17",
                         "5", "990", "6", "991", "3", "991", "0",
                         "2", "6", "991", "3", "993", "0", "0", "0"], build)
        assert "Hiện tại:" in out and "Mã sách đã tồn tại" in out
        assert "Cập nhật thông tin sách thành công" in out
        rows = lines(data)
        assert "BOOK|S991|QA Book Edited|QA Author|300.0|2|Chưa phân loại|100.0" in rows
        assert "CUSTOMER|KH993|Lan An QA|0901113333" in rows
        assert not any(row.startswith("BOOK|S990|") or row.startswith("CUSTOMER|KH991|") for row in rows)
        print("PASS single-field book edits, cancel, duplicate ID and ID changes")

        payment = work / "payment.txt"
        shutil.copy2(data, payment)
        shutil.copy2(Path(str(data) + ".accounts"), Path(str(payment) + ".accounts"))
        payment.write_text(payment.read_text(encoding="utf-8").replace(
            "BOOK|S017|Lap Trinh C++|Bjarne Stroustrup|170000.0|9|",
            "BOOK|S017|Lap Trinh C++|Bjarne Stroustrup|170000.0|10|"), encoding="utf-8")
        before = lines(payment)
        assert any(row.startswith("BOOK|S017|") and "|170000.0|10|" in row for row in before)
        out = run(payment, ["qaadmin", "admin123", "5", "990", "990", "1", "17", "1", "K", "OK",
                            "5", "991", "990", "1", "17", "1", "K", "HUY", "0", "0"], build)
        assert "THANH TOÁN THÀNH CÔNG" in out and "Đã hủy hóa đơn" in out
        after = lines(payment)
        assert any(row.startswith("BOOK|S017|") and "|170000.0|9|" in row for row in after)
        assert sum(row.startswith("INVOICE|HD990|") for row in after) == 1
        assert sum(row.startswith("DETAIL|HD990|S017|") and "|1|170000.0" in row for row in after) == 1
        assert not any(row.startswith("INVOICE|HD991|") for row in after)
        out = run(payment, ["qaadmin", "admin123", "6", "2", "990", "0", "2", "8", "990", "0", "0", "0"], build)
        assert "HÓA ĐƠN HD990" in out and "Tổng tiền: 170,000 VNĐ" in out
        assert "S017" in out and "KH990" in out
        print("PASS payment, cancellation, invoice lookup and restart")

        out = run(payment, ["qaadmin", "admin123", "1", "5", "17", "6", "995", "0",
                            "2", "6", "990", "3", "998",
                            "13", "2", "997", "QA Duplicate A", "0909990000",
                            "998", "QA Duplicate B", "0909990000", "0", "0", "0"], build)
        assert "Không thể đổi mã sách đã phát sinh hóa đơn" in out
        assert "Không thể đổi mã khách hàng đã phát sinh hóa đơn" in out
        assert "Số điện thoại khách hàng đã tồn tại" in out
        assert any(row.startswith("BOOK|S017|") for row in lines(payment))
        assert any(row.startswith("CUSTOMER|KH990|") for row in lines(payment))
        assert not any(row.startswith("CUSTOMER|KH997|") or row.startswith("CUSTOMER|KH998|") for row in lines(payment))
        print("PASS transaction ID immutability and duplicate phone within bulk input")

        today = date.today()
        day = today.strftime("%d/%m/%Y")
        report_steps = ["1", "2", day, "3", str(today.month), str(today.year), "4", str(today.year),
                        "5", day, day]
        for choice in range(6, 12):
            report_steps += [str(choice), "6"]
        out = run(payment, ["qaadmin", "admin123", "8", *report_steps, "0", "0", "0"], build)
        assert out.count("THỐNG KÊ DOANH THU:") == 11
        assert "XẾP HẠNG NHÀ CUNG CẤP" in out and "DOANH THU THEO KHÁCH HÀNG" in out
        print("PASS all 11 revenue menu selections")

        out = run(payment, ["qaadmin", "admin123", "7", "1", "2", "17", "3", "Lap",
                            "6", "7", "8", "9", "5", "4", "17", "990", "1", "QA receipt", "DONGY",
                            "5", "17", "9", "QA count", "DONGY", "0",
                            "4", "4", "990", "0", "0", "0"], build)
        assert "Nhập hàng từ nhà cung cấp thành công" in out
        assert "Điều chỉnh tồn kho thành công" in out
        assert "Không thể xóa nhà cung cấp đã có nguồn cung" in out
        assert any(row.startswith("BOOK|S017|") and "|170000.0|9|" in row for row in lines(payment))
        print("PASS inventory menu and referenced supplier protection")

        out = run(payment, ["qaadmin", "admin123",
                            "3", "1", "992", "QA Cashier", "0903334444", "2", "0",
                            "9", "2", "992", "qaemployee2", "employee123", "employee123",
                            "3", "A002", "4", "A002",
                            "6", "A002", "qaemployee2new", "2", "992", "0", "0",
                            "qaemployee2new", "employee123",
                            "1", "1", "2", "17", "3", "Lap", "4", "0", "200000", "5", "1", "0",
                            "2", "1", "2", "990", "3", "An", "4", "994", "An Employee", "0904445555",
                            "5", "994", "1", "An Employee Updated", "0",
                            "3", "992", "990", "17", "1", "K", "HUY",
                            "4", "5", "6", "employee123", "employee456", "employee456", "0",
                            "qaemployee2new", "employee456", "0", "0"], build)
        assert "Đã khóa tài khoản" in out and "Đã mở khóa tài khoản" in out
        assert "Đã cập nhật tài khoản" in out
        assert out.count("Đăng nhập thành công. Xin chào qaemployee2new") == 2
        assert "Đổi mật khẩu thành công" in out and "Đã hủy hóa đơn" in out
        assert "CUSTOMER|KH994|An Employee Updated|0904445555" in lines(payment)
        assert not any(row.startswith("INVOICE|HD992|") for row in lines(payment))
        print("PASS employee top-level menus and account lock, unlock, rename, password change")

        out = run(payment, ["qaadmin", "admin123", "2", "2", "3", "990", "4", "An", "5", "0901114444",
                            "8", "990", "9", "990", "10", "990", "11", "990", "12",
                            "13", "2", "995", "QA Bulk A", "0905556666", "996", "QA Bulk B", "0905557777",
                            "14", "7", "995", "0", "0", "0"], build)
        assert "Đã thêm 2 khách hàng" in out and "Lựa chọn không hợp lệ" in out
        assert "Khởi tạo/thay danh sách" not in out
        assert "Xóa khách hàng thành công" in out
        assert not any(row.startswith("CUSTOMER|KH995|") for row in lines(payment))
        assert any(row.startswith("CUSTOMER|KH996|") for row in lines(payment))
        print("PASS customer history, statistics, bulk add, removed option and delete")

        out = run(payment, ["qaadmin", "admin123", "6", "1", "2", "990", "3", "990", "4", "990",
                            "5", day, "6", str(today.month), str(today.year), "7", str(today.year),
                            "8", day, day, "9", "10", "990", "0", "0", "0"], build)
        assert out.count("Tìm/lọc hóa đơn thành công") >= 10
        assert "HÓA ĐƠN HD990" in out
        print("PASS all 10 invoice management menu selections")

        out = run(payment, ["qaadmin", "admin123", "1", "2", "3", "991", "4", "QA",
                            "7", "0", "1000000", "8", "9", "2",
                            "992", "QA Book Two", "QA Author", "50", "100", "3",
                            "993", "QA Book Three", "QA Author", "60", "120", "4",
                            "6", "993", "0",
                            "3", "2", "3", "1", "4", "QA", "5", "090", "6", "Thu ngân", "9",
                            "10", "2", "993", "QA Bulk Emp A", "0906667777", "2",
                            "994", "QA Bulk Emp B", "0906668888", "2",
                            "7", "993", "QA Bulk Emp Revised", "0906667777", "2", "8", "994", "0",
                            "4", "1", "3", "990", "QA Supplier Updated", "0912345678",
                            "2", "991", "QA Supplier Delete", "0919998888", "4", "991", "DONGY", "0",
                            "9", "1", "5", "A002", "reset123", "reset123", "0",
                            "10", "admin123", "admin456", "admin456", "11", "0",
                            "qaadmin", "admin456", "0", "0"], build)
        assert "Đã thêm 2 sách" in out and "Xóa sách thành công" in out
        assert "Đã thêm 2 nhân viên" in out and "Xóa nhân viên và tài khoản liên kết thành công" in out
        assert "Cập nhật nhà cung cấp thành công" in out and "Xóa nhà cung cấp thành công" in out
        assert "Đã đặt mật khẩu mới" in out and "Đổi mật khẩu thành công" in out
        assert "Đăng nhập thành công. Xin chào qaadmin" in out.split("Đổi mật khẩu thành công", 1)[1]
        rows = lines(payment)
        assert any(row.startswith("BOOK|S992|") for row in rows) and not any(row.startswith("BOOK|S993|") for row in rows)
        assert any(row.startswith("EMPLOYEE|NV993|QA Bulk Emp Revised|") for row in rows)
        assert not any(row.startswith("EMPLOYEE|NV994|") or row.startswith("SUPPLIER|NCC991|") for row in rows)
        print("PASS remaining book, employee, supplier and account menu operations")

        run_remaining(work, build, run, JAVA)


if __name__ == "__main__":
    main()
