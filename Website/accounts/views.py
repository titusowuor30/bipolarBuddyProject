from django.shortcuts import render, redirect
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
            user = form.save()
            login(request, user)
            return redirect('home')  # Redirect to a success page
    else:
        form = CustomUserCreationForm()
    return render(request, 'signup.html', {'form': form})

def log_out(request):
    logout(request)
    return redirect('login')


