<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: Content-Type');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    exit;
}

require_once __DIR__ . '/db.php';

function parsePgArray($pgArray) {
    if (!$pgArray || $pgArray === '{}') return [];
    $clean = trim($pgArray, '{}');
    if ($clean === '') return [];
    return str_getcsv($clean, ',', '"');
}

function respond($data, $status = 200) {
    http_response_code($status);
    echo json_encode($data);
    exit;
}

function requestData() {
    $body = json_decode(file_get_contents('php://input'), true);
    return is_array($body) ? $body : [];
}

$resource = $_GET['resource'] ?? '';

try {
    // Ensure schedule_submission table exists
    $conn->exec("
        CREATE TABLE IF NOT EXISTS schedule_submission (
            student_id VARCHAR(50) PRIMARY KEY REFERENCES account(student_id) ON DELETE CASCADE,
            is_submitted BOOLEAN NOT NULL DEFAULT FALSE,
            submitted_at TIMESTAMPTZ
        )
    ");

    if ($_SERVER['REQUEST_METHOD'] === 'GET') {
        if ($resource === 'enrolled_courses') {
            $studentId = $_GET['studentId'] ?? '';
            if (!$studentId) respond(['success' => false, 'message' => 'Student ID required'], 400);

            $sql = "SELECT DISTINCT sub.subject_code AS \"courseCode\", sub.subject_name AS \"courseName\",
                           COALESCE(s_schedule.section_name, s_offering.section_name) AS section,
                           sc.day_of_the_week AS day,
                           CONCAT(sc.start_time, ' - ', sc.end_time) AS \"timeRange\",
                           CONCAT_WS(', ', NULLIF(TRIM(r.building), ''), NULLIF(TRIM(r.room_num), '')) AS room,
                           i.instructor_name AS instructor,
                           o.class_type AS type,
                           o.offering_id
                    FROM plotting p
                    JOIN offering o ON o.offering_id = p.offering_id
                    JOIN subject sub ON sub.subject_id = o.subject_id
                    LEFT JOIN schedule sc ON sc.schedule_id = o.schedule_id
                    LEFT JOIN section s_schedule ON s_schedule.section_id = sc.section_id
                    LEFT JOIN section s_offering ON s_offering.section_id = o.section_id
                    LEFT JOIN instructor i ON i.instructor_id = o.instructor_id
                    LEFT JOIN room r ON r.room_id = o.room_id
                    WHERE TRIM(p.student_id) = TRIM(:studentId)
                    ORDER BY 5";

            $stmt = $conn->prepare($sql);
            $stmt->execute([':studentId' => $studentId]);
            $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

            $formatted = [];
            foreach ($rows as $row) {
                $formatted[] = [
                    'courseCode' => $row['courseCode'],
                    'courseName' => $row['courseName'],
                    'section' => $row['section'] ?: 'Unassigned',
                    'day' => $row['day'] ?: 'TBA',
                    'timeRange' => $row['timeRange'] ?: 'TBA',
                    'room' => $row['room'] ?: 'TBA',
                    'instructor' => $row['instructor'] ?: 'TBA',
                    'type' => $row['type'] ?: 'Lecture',
                    'offeringIds' => [(int)$row['offering_id']]
                ];
            }
            respond($formatted);
        }

        if ($resource === 'submission_status') {
            $studentId = $_GET['studentId'] ?? '';
            if (!$studentId) respond(['success' => false, 'message' => 'Student ID required'], 400);

            $stmt = $conn->prepare("SELECT is_submitted, submitted_at FROM schedule_submission WHERE TRIM(student_id) = TRIM(:studentId)");
            $stmt->execute([':studentId' => $studentId]);
            $row = $stmt->fetch(PDO::FETCH_ASSOC);

            if ($row) {
                respond([
                    'isSubmitted' => (bool)$row['is_submitted'],
                    'submittedAt' => $row['submitted_at']
                ]);
            } else {
                respond([
                    'isSubmitted' => false,
                    'submittedAt' => null
                ]);
            }
        }

        respond(['success' => false, 'message' => 'Unknown GET resource'], 404);
    }

    $data = requestData();

    if ($resource === 'submit_schedule') {
        $studentId = $data['studentId'] ?? '';
        if (!$studentId) {
            respond(['success' => false, 'message' => 'Student ID is required'], 400);
        }

        try {
            $stmt = $conn->prepare("
                INSERT INTO schedule_submission (student_id, is_submitted, submitted_at)
                VALUES (:studentId, TRUE, CURRENT_TIMESTAMP)
                ON CONFLICT (student_id) DO UPDATE
                SET is_submitted = TRUE, submitted_at = CURRENT_TIMESTAMP
            ");
            $stmt->execute([':studentId' => $studentId]);

            $updatePlotting = $conn->prepare("UPDATE plotting SET status = 'Submitted' WHERE TRIM(student_id) = TRIM(:studentId)");
            $updatePlotting->execute([':studentId' => $studentId]);

            respond(['success' => true, 'message' => 'Schedule successfully submitted to Admin']);
        } catch (Exception $e) {
            respond(['success' => false, 'message' => $e->getMessage()], 500);
        }
    }

    if ($resource === 'enroll') {
        $studentId = $data['studentId'] ?? '';
        $offeringIds = $data['offeringIds'] ?? [];
        if (!$studentId || empty($offeringIds)) {
            respond(['success' => false, 'message' => 'Student ID and Offering IDs are required'], 400);
        }

        $conn->beginTransaction();
        try {
            $deleteStmt = $conn->prepare("DELETE FROM plotting WHERE TRIM(student_id) = TRIM(:studentId) AND offering_id = :offeringId");
            $insertStmt = $conn->prepare("INSERT INTO plotting (student_id, offering_id, status) VALUES (:studentId, :offeringId, 'Draft')");

            foreach ($offeringIds as $offeringId) {
                $deleteStmt->execute([':studentId' => $studentId, ':offeringId' => $offeringId]);
                $insertStmt->execute([':studentId' => $studentId, ':offeringId' => $offeringId]);
            }

            // Reset schedule submission status to false when modifying schedule
            $resetSubmission = $conn->prepare("
                INSERT INTO schedule_submission (student_id, is_submitted, submitted_at)
                VALUES (:studentId, FALSE, NULL)
                ON CONFLICT (student_id) DO UPDATE
                SET is_submitted = FALSE, submitted_at = NULL
            ");
            $resetSubmission->execute([':studentId' => $studentId]);

            $conn->commit();
            respond(['success' => true]);
        } catch (Exception $e) {
            if ($conn->inTransaction()) $conn->rollBack();
            respond(['success' => false, 'message' => $e->getMessage()], 500);
        }
    }

    if ($resource === 'unenroll') {
        $studentId = $data['studentId'] ?? '';
        $offeringIds = $data['offeringIds'] ?? [];
        if (!$studentId || empty($offeringIds)) {
            respond(['success' => false, 'message' => 'Student ID and Offering IDs are required'], 400);
        }

        $conn->beginTransaction();
        try {
            $stmt = $conn->prepare("DELETE FROM plotting WHERE TRIM(student_id) = TRIM(:studentId) AND offering_id = :offeringId");
            foreach ($offeringIds as $offeringId) {
                $stmt->execute([':studentId' => $studentId, ':offeringId' => $offeringId]);
            }

            // Reset schedule submission status
            $resetSubmission = $conn->prepare("
                INSERT INTO schedule_submission (student_id, is_submitted, submitted_at)
                VALUES (:studentId, FALSE, NULL)
                ON CONFLICT (student_id) DO UPDATE
                SET is_submitted = FALSE, submitted_at = NULL
            ");
            $resetSubmission->execute([':studentId' => $studentId]);

            $conn->commit();
            respond(['success' => true]);
        } catch (Exception $e) {
            if ($conn->inTransaction()) $conn->rollBack();
            respond(['success' => false, 'message' => $e->getMessage()], 500);
        }
    }

    respond(['success' => false, 'message' => 'Unknown POST resource'], 404);
} catch (Throwable $e) {
    if (isset($conn) && $conn->inTransaction()) {
        $conn->rollBack();
    }
    respond(['success' => false, 'message' => $e->getMessage()], 500);
}
?>