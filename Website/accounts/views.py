from django.shortcuts import render, redirect,get_object_or_404
from django.contrib.auth import login, authenticate,logout
from django.contrib.auth.forms import AuthenticationForm
from .forms import CustomUserCreationForm, LoginForm
from django.contrib import messages

from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import AllowAny

from django.contrib.auth.views import LoginView
from django.urls import reverse_lazy
from .forms import CustomAuthenticationForm
from rest_framework import viewsets, status
from rest_framework.response import Response
from rest_framework import permissions
from .models import *
from appointments.models import *
from core.models import *
from core.forms import *
from .serializers import *
from django.contrib.auth.models import Group
from django.shortcuts import render, redirect, get_object_or_404
from django.contrib.auth.decorators import login_required
from django.db import transaction


@login_required
def add_prescription(request):
    if request.method == 'POST':
        form = PrescriptionForm(request.POST)
        if form.is_valid():
            prescription = form.save(commit=False)  # Don't save to database yet
            prescription.doctor = request.user.doctor  # Assign the current doctor
            prescription.save()  # Now save the object to the database
            return redirect('doctor_profile')
    else:
        form = PrescriptionForm()
    return redirect('doctor_profile')

@login_required
def edit_prescription(request, pk):
    print(request.user.doctor.id)
    prescription = get_object_or_404(Prescription, pk=pk)
    if request.method == 'POST':
        form = PrescriptionForm(request.POST, instance=prescription)
        if form.is_valid():
            form.save()
            return redirect('doctor_profile')
    return redirect('doctor_profile')

@login_required
def delete_prescription(request, pk):
    prescription = get_object_or_404(Prescription, pk=pk)
    prescription.delete()
    return redirect('doctor_profile')

def doctors(request):
    docts=Doctor.objects.all()
    return render(request,'accounts/doctors.html',{"doctors": docts})

#android login endpoint
class LoginAPIView(APIView):
    permission_classes = [AllowAny]

    def post(self, request):
        form = LoginForm(request, data=request.data)
        if form.is_valid():
            email = form.cleaned_data.get('username')
            password = form.cleaned_data.get('password')
            user = authenticate(request, username=email, password=password)
            if user is not None:
                login(request, user)
                messages.success(request, "Login successful!")
                return Response({'success': True, 'message': 'Login successful!'}, status=status.HTTP_200_OK)
            else:
                return Response({'success': False, 'message': 'Login failed. Please check credentials and try again!'}, status=status.HTTP_401_UNAUTHORIZED)
        else:
            return Response({'success': False, 'errors': form.errors}, status=status.HTTP_400_BAD_REQUEST)

class LoginView(LoginView):
    form_class = CustomAuthenticationForm
    template_name = 'accounts/login.html'
    success_url = reverse_lazy('home')

    def form_valid(self, form):
        messages.success(self.request, "You have successfully logged in.")
        return super().form_valid(form)

    def form_invalid(self, form):
        messages.error(self.request, "Invalid email or password. Please try again.")
        return super().form_invalid(form)

def signup_view(request):
    if request.method == 'POST':
        form = CustomUserCreationForm(request.POST)
        if form.is_valid():
            messages.success(request,"Your account has been created successfully!")
            user = form.save()
            login(request, user)
            return redirect('home')  # Redirect to a success page
    else:
        form = CustomUserCreationForm()
    return render(request, 'accounts/signup.html', {'form': form})

def log_out(request):
    logout(request)
    return redirect('login')


class PatientSignupViewSet(viewsets.ModelViewSet):
    queryset = Patient.objects.all()
    serializer_class = PatientSerializer
    authentication_classes = []
    permission_classes = []

    @transaction.atomic
    def create(self, request, *args, **kwargs):
        # Extract data
        user_data = {
            'email': request.data.get('email'),
            'username': request.data.get('username'),
            'password': request.data.get('password'),
            'first_name': request.data.get('first_name'),
            'last_name': request.data.get('last_name'),
            'phone': request.data.get('phone'),
            'age': request.data.get('age'),
            'height': request.data.get('height'),
            'weight': request.data.get('weight'),
            'gender': request.data.get('gender'),
        }

       # Create CustomUser instance
        user_serializer = CustomUserSerializer(data=user_data)
        user_serializer.is_valid(raise_exception=True)
        user = user_serializer.save()

        # Add the user to 'patients' group
        pg, created = Group.objects.get_or_create(name='patients')
        user.groups.add(pg)
        user.save()

        # Create Patient instance
        patient,create=Patient.objects.get_or_create(user=user,doctor=None)
        # Return response
        return Response({
            'user': user_serializer.data,
            'patient': {
                'id': patient.id,
                'doctor': None
            }
        }, status=status.HTTP_201_CREATED)

class DoctorViewSet(viewsets.ModelViewSet):
    queryset = Doctor.objects.all()
    serializer_class = DoctorSerializer
    permission_classes = []
    authentication_classes = []

    def get_queryset(self):
        queryset = super().get_queryset()
        department_id = self.request.query_params.get('department')
        if department_id:
            queryset = queryset.filter(id=department_id)
        return queryset
    
def patient_profile(request):
    user = request.user
    appointments = Appointment.objects.filter(user=user).order_by('date')
    
    context = {
        'user': user,
        'patient':Patient.objects.get(user=user),
        'appointments': appointments
    }
    return render(request, 'accounts/patient_profile.html', context)

def doctor_profile(request):
    user = request.user
    doctor = get_object_or_404(Doctor, user=user)
    patients = Patient.objects.filter(doctor=doctor)
    vitals = Vitals.objects.filter(patient__in=patients)
    tremors = Tremors.objects.filter(patient__in=patients)
    prescriptions = Prescription.objects.filter(patient__in=patients)
    appointments = Appointment.objects.filter(doctor=doctor)# SELCT * FROM accounts_appointments WHERE doctor_id=2;
    context = {
        'user': user,
        'doctor': doctor,
        'patients': patients,
        'vitals': vitals,
        'tremors': tremors,
        'prescriptions': prescriptions,
        'appointments': appointments
    }
    return render(request, 'accounts/doctor_profile.html', context)

def kin_profile(request):
    user = request.user
    patient_kin = get_object_or_404(PatientKin, user=user)
    patient = patient_kin.patient
    vitals = Vitals.objects.filter(patient=patient)
    tremors = Tremors.objects.filter(patient=patient)
    prescriptions = Prescription.objects.filter(patient=patient)
    context = {
        'user': user,
        'vitals': vitals,
        'tremors': tremors,
        'prescriptions': prescriptions
    }
    return render(request, 'accounts/kin_profile.html', context)

