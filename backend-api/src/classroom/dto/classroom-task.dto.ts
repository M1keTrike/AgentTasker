export class ClassroomTaskDto {
  id: string;
  courseId: string;
  courseName: string;
  title: string;
  description?: string;
  dueDate?: string;
  submissionState: string;
  alternateLink?: string;
  maxPoints?: number;
}
