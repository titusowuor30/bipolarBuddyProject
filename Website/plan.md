# user/patient-login, heart rate, bp, oxygen circulation, tremors, reports, view medication and other notificationskin-login, reports, alarms/notifications on patients doctors-login, prescribing medications, alarms/notifications on patients

# Db Plan
- Key players (Patient,Patient Kin,Doctor,Prescriptions,Notifications,Reminders,ContactInfo,PatientVitals,Medical History)

### Patient table
class Patient(models.Model):
- id(auto) - PK
- doctor(FK to Doctor(id))
- user details(first name,email, last name,dob,national_id,phone)
- contact (Many to Many(ContactInfo))

### Patient Kin
- id(PK)
- patient(FK to Patient(id))
- user details(first name,email, last name,dob,national_id,phone)
- relation

### Doctor
- id(PK)
- user details(first name,email, last name,dob,national_id,phone)
- contact (Many to Many(ContactInfo))
- specialization

### PatientVitals
- id(PK)
- patient(FK to Patient(id))
- heart_rate
- blood_pressure
- o2_saturation
- respiration_rate
- tremors(episodes)
- timestamp(Date Time)

### Prescription
- id(PK)
- doctor(FK to Doctor(id))
- patient(FK to Patient(id))
- medication_name 
- dosage
- created_at

### Notification
- id(PK)
- user(Fk to user(id))
- title
- message
- created_at

### Misc . Tables
- Medical History(optional)
- Reminders


### List of reports
- vitals reports
- medical history
- 

