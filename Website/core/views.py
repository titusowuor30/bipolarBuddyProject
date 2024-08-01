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
        useremail = request.data.get('user')
        print(request.data)
        if not useremail:
            return Response({"error": "User email is required."}, status=status.HTTP_400_BAD_REQUEST)
        
        patient = Patient.objects.filter(Q(user__email=useremail) | Q(user__username=useremail)).first()
        if not patient:
            return Response({"error": "Patient not found."}, status=status.HTTP_404_NOT_FOUND)
        
        # Get the existing vitals entry for the patient if it exists
        vitals_instance = Vitals.objects.filter(patient=patient).first()
        if vitals_instance:
            request.data['patient'] = patient.id
            if vitals_instance.heart_rate !="" and vitals_instance.respiration_rate !="" and vitals_instance.blood_pressure !="" and vitals_instance !="":
                serializer = self.get_serializer(data=request.data)
            else:
                request.data['patient'] = patient.id
                serializer = self.get_serializer(vitals_instance, data=request.data, partial=True)
        
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
        serializer = self.get_serializer(vitals_instance, data=request.data, partial=partial)
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
        # Ensure the request data is correctly formatted
        print(request.data)
        serializer = self.get_serializer(data=request.data)
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

