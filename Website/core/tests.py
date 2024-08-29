from django.test import TestCase
from rest_framework.test import APIClient
from rest_framework import status
from django.contrib.auth.models import User
from .models import Patient, Vitals
from datetime import datetime, timedelta

class VitalsViewSetTests(TestCase):
    def setUp(self):
        self.client = APIClient()
        self.user = User.objects.create_user(username='janedoe', email='janedoe@example.com', password='password')
        self.patient = Patient.objects.create(user=self.user)
        self.vitals_url = '/api/vitals/'
        self.valid_payload = {
            'heart_rate': 70,
            'blood_pressure': '120/80',
            'respiration_rate': 16,
            'o2_saturation': 98,
            'user': 'janedoe@example.com',
            'timestamp': datetime.now().isoformat()
        }

    def test_create_vitals(self):
        # Test creating a new vitals entry
        response = self.client.post(self.vitals_url, self.valid_payload, format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(Vitals.objects.count(), 1)

    def test_create_vitals_missing_user(self):
        # Test creating vitals entry with missing user email
        invalid_payload = self.valid_payload.copy()
        invalid_payload.pop('user')
        response = self.client.post(self.vitals_url, invalid_payload, format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(Vitals.objects.count(), 0)

    def test_create_vitals_missing_vitals(self):
        # Test creating vitals entry with missing required vitals
        invalid_payload = self.valid_payload.copy()
        invalid_payload.pop('heart_rate')
        response = self.client.post(self.vitals_url, invalid_payload, format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(Vitals.objects.count(), 0)

    def test_update_existing_vitals(self):
        # Test updating an existing vitals entry with the same timestamp
        vitals_instance = Vitals.objects.create(
            patient=self.patient,
            heart_rate=70,
            blood_pressure='120/80',
            respiration_rate=16,
            o2_saturation=98,
            timestamp=datetime.now()
        )
        updated_payload = self.valid_payload.copy()
        updated_payload['heart_rate'] = 75
        updated_payload['timestamp'] = vitals_instance.timestamp.isoformat()
        
        response = self.client.put(f'{self.vitals_url}{vitals_instance.id}/', updated_payload, format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        vitals_instance.refresh_from_db()
        self.assertEqual(vitals_instance.heart_rate, 75)

    def test_create_new_vitals_with_different_timestamp(self):
        # Test creating a new vitals entry with a different timestamp
        Vitals.objects.create(
            patient=self.patient,
            heart_rate=70,
            blood_pressure='120/80',
            respiration_rate=16,
            o2_saturation=98,
            timestamp=datetime.now()
        )
        new_payload = self.valid_payload.copy()
        new_payload['timestamp'] = (datetime.now() + timedelta(minutes=5)).isoformat()

        response = self.client.post(self.vitals_url, new_payload, format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(Vitals.objects.count(), 2)

    def test_update_vitals_with_different_timestamp(self):
        # Test trying to update an existing vitals entry with a different timestamp (should create new entry)
        vitals_instance = Vitals.objects.create(
            patient=self.patient,
            heart_rate=70,
            blood_pressure='120/80',
            respiration_rate=16,
            o2_saturation=98,
            timestamp=datetime.now()
        )
        updated_payload = self.valid_payload.copy()
        updated_payload['heart_rate'] = 75
        updated_payload['timestamp'] = (vitals_instance.timestamp + timedelta(minutes=5)).isoformat()
        
        response = self.client.put(f'{self.vitals_url}{vitals_instance.id}/', updated_payload, format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(Vitals.objects.count(), 2)

    def test_update_nonexistent_vitals(self):
        # Test trying to update a vitals entry that does not exist
        updated_payload = self.valid_payload.copy()
        response = self.client.put(f'{self.vitals_url}999/', updated_payload, format='json')
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_create_vitals_for_nonexistent_patient(self):
        # Test creating a vitals entry for a non-existent patient
        non_existent_user_payload = self.valid_payload.copy()
        non_existent_user_payload['user'] = 'nonexistent@example.com'
        response = self.client.post(self.vitals_url, non_existent_user_payload, format='json')
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        self.assertEqual(Vitals.objects.count(), 0)

