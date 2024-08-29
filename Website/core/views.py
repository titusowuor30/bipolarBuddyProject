from django.shortcuts import render
from django.http import HttpResponse
from rest_framework.views import APIView
from rest_framework import viewsets
from .serializers import VitalsSerializer
from rest_framework.response import Response
from rest_framework import status
from rest_framework import permissions
from rest_framework.authentication import BasicAuthentication,TokenAuthentication
from .models import *
from accounts.models import Departments
from .serializers import *
from django.db.models import Q
from django.shortcuts import render
from .models import Vitals
import json
from django.contrib.auth import get_user_model
from django.db.models import Q
from datetime import datetime

User=get_user_model()

def patient_vital_trends(request, patient_id):
    # Retrieve all vitals for the patient
    vitals = Vitals.objects.filter(patient_id=patient_id).order_by('timestamp')
    patient=Patient.objects.get(id=patient_id)

    # Prepare data for graphing
    timestamps = []
    heart_rates = []
    systolic_bp = []
    diastolic_bp = []
    o2_saturations = []
    respiration_rates = []

    for vital in vitals:
        timestamps.append(vital.timestamp.strftime('%Y-%m-%d %H:%M:%S'))

        # Convert heart_rate and respiration_rate to integers, replacing null with 0
        heart_rates.append(int(vital.heart_rate) if vital.heart_rate else 0)
        respiration_rates.append(int(vital.respiration_rate) if vital.respiration_rate else 0)

        # Split blood_pressure into systolic and diastolic, replacing null with 0
        if vital.blood_pressure:
            bp_values = vital.blood_pressure.split('/')
            if len(bp_values) == 2:
                systolic_bp.append(int(bp_values[0]) if bp_values[0] else 0)
                diastolic_bp.append(int(bp_values[1]) if bp_values[1] else 0)
            else:
                systolic_bp.append(0)
                diastolic_bp.append(0)
        else:
            systolic_bp.append(0)
            diastolic_bp.append(0)

        # Convert o2_saturation to integers, replacing null with 0
        o2_saturations.append(int(vital.o2_saturation) if vital.o2_saturation else 0)

    context = {
        'patient': patient,  # Assuming there's at least one vital record
        'timestamps': json.dumps(timestamps),
        'heart_rates': json.dumps(heart_rates),
        'systolic_bp': json.dumps(systolic_bp),
        'diastolic_bp': json.dumps(diastolic_bp),
        'o2_saturations': json.dumps(o2_saturations),
        'respiration_rates': json.dumps(respiration_rates),
    }

    print(context)

    return render(request, 'core/patient_vital_trends.html', context)


# Create your views here.
def home(request):
    return render(request, 'core/index.html', {"name":"Wambui"})

def contact(request):
    return render(request, 'core/contact.html')


class VitalsViewSet(viewsets.ModelViewSet):  # CRUD
    queryset = Vitals.objects.all()
    serializer_class = VitalsSerializer
    permission_classes = []
    authentication_classes = []

    def create(self, request, *args, **kwargs):
        print(request.data)
        # Check if all required vitals are provided
        vitals_data = {
            'heart_rate': request.data.get('heart_rate',''),
            'respiration_rate': request.data.get('respiration_rate',''),
            'o2_saturation': request.data.get('o2_saturation',''),
            'blood_pressure': request.data.get('blood_pressure',''),
        }
        useremail = request.data.get('user')
        if not useremail:
            return Response({"error": "User email is required."}, status=status.HTTP_400_BAD_REQUEST)
        
        patient = Patient.objects.filter(Q(user__email=useremail) | Q(user__username=useremail)).first()
        if not patient:
            return Response({"error": "Patient not found."}, status=status.HTTP_404_NOT_FOUND)
        
        vitals_data['patient'] = patient.id
        serializer = self.get_serializer(data=request.data)
        
        # Get the existing vitals entry for the patient if it exists
        vitals_instance = Vitals.objects.filter(patient=patient).order_by('-timestamp').first()

        
        print(vitals_data)
        if not all(vitals_data.values()):
            return Response({"error": "All vital fields and timestamp are required."}, status=status.HTTP_400_BAD_REQUEST)
        
        # If existing vitals entry is found, check timestamp
        if vitals_instance:
            if vitals_instance.timestamp != datetime.now() and vitals_instance.heart_rate !="" and vitals_instance.respiration_rate !="" and vitals_instance.blood_pressure !="" and vitals_instance !="":
                # Create new vitals entry
                serializer = self.get_serializer(data=vitals_data)
            else:
                 # Update existing vitals entry
                serializer = self.get_serializer(vitals_instance, data=request.data, partial=True)
        else:
            # Create new vitals entry
            serializer = self.get_serializer(data=vitals_data)

        serializer.is_valid(raise_exception=True)
        self.perform_create(serializer)
        headers = self.get_success_headers(serializer.data)
        return Response(serializer.data, status=status.HTTP_201_CREATED, headers=headers)

    def update(self, request, *args, **kwargs):
        useremail = request.data.get('email')
        if not useremail:
            return Response({"error": "User email is required."}, status=status.HTTP_400_BAD_REQUEST)

        patient = Patient.objects.filter(user__email=useremail).first()
        if not patient:
            return Response({"error": "Patient not found."}, status=status.HTTP_404_NOT_FOUND)
        
        vitals_instance = Vitals.objects.filter(patient__user__email=useremail).first()
        if not vitals_instance:
            return Response({"error": "Vitals entry not found."}, status=status.HTTP_404_NOT_FOUND)
        
        partial = kwargs.pop('partial', False)
        request.data['patient'] = patient.id
        
        # Check if all required vitals are provided
        vitals_data = {
            'heart_rate': request.data.get('heart_rate'),
            'respiration_rate': request.data.get('respiration_rate'),
            'blood_pressure': request.data.get('blood_pressure'),
            'timestamp': request.data.get('timestamp')
        }
        
        if not all(vitals_data.values()):
            return Response({"error": "All vital fields and timestamp are required."}, status=status.HTTP_400_BAD_REQUEST)
        
        if vitals_instance.timestamp == vitals_data['timestamp']:
            # Update existing vitals entry
            serializer = self.get_serializer(vitals_instance, data=request.data, partial=partial)
        else:
            # Create new vitals entry
            serializer = self.get_serializer(data=request.data)
        
        serializer.is_valid(raise_exception=True)
        self.perform_update(serializer)

        if getattr(vitals_instance, '_prefetched_objects_cache', None):
            # If 'prefetch_related' has been applied to a queryset, we need to invalidate the prefetch cache on the instance.
            vitals_instance._prefetched_objects_cache = {}

        return Response(serializer.data)

class TremorsViewSet(viewsets.ModelViewSet):
    queryset = Tremors.objects.all()
    serializer_class = TremorsSerializer
    permission_classes = []
    authentication_classes = []

    def create(self, request, *args, **kwargs):
        # Ensure the request data is correctly formatte
        print(request.data.get('user_email'))
        user=User.objects.filter(Q(username=request.data.get('user_email'))|Q(email=request.data.get('user_email'))).first()
        print(user)
        user_email=None
        if user is not None:
            user_email=user.email
        else:
            user_email=request.data.get('user_email')
        serializer = self.get_serializer(data={'user_email':user_email})
        if serializer.is_valid():
            # Save the tremor entry
            self.perform_create(serializer)
            headers = self.get_success_headers(serializer.data)
            return Response(serializer.data, status=status.HTTP_201_CREATED, headers=headers)
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)
    


def departments(request):
    departments=Departments.objects.all()
    return render(request,'core/departments.html',{'departments':departments})

class DepartmentViewSet(viewsets.ReadOnlyModelViewSet):
    queryset = Departments.objects.all()
    serializer_class = DepartmentSerializer
    permission_classes = []
    authentication_classes = []

