                                                                                                     
variable "ami_name" {                                                                                
  type    = string                                                                                   
  default = "bharat-1"                                                                  
}                                                                                                    
                                                                                                     
variable "region" {                                                                                  
  type    = string                                                                                   
  default = "us-west-2"                                                                              
}                                                                                                    

variable "ssh_username" {                                                                            
  type    = string                                                                                   
  default = "centos"                                                                                 
}                                                                                                    
                                                                                                     
variable "base_ami" {                                                                                
  type    = string                                                                                   
  default = "ami-b63ae0ce"                                                                           
}                                                                                                    
                                                                                                     
variable "instance_size" {                                                                           
  type    = string                                                                                   
  default = "t3.small"                                                                               
}                                                                                                    
                                                                                                     
variable "subnet_id" {                                                                               
  type    = string                                                                                   
  default = "subnet-6553f513"                                                                        
}                                                                                                    
                                                                                                     
variable "vpc_id" {                                                                                  
  type    = string                                                                                   
  default = "vpc-0fe36f6b"                                                                           
}                                                                                                    
                                                                                                     
variable "security_group_id" {                                                                       
  type    = string                                                                                   
  default = "sg-139dde6c"                                                                            
}                                                                                                    
                                                                                                     
source "amazon-ebs" "autogenerated_1" {                                                              
  ami_name      = "${var.ami_name}"                                                                  
  instance_type = "${var.instance_size}"                                                           
  region        = "${var.region}"                                                             
  source_ami    = "${var.base_ami}"                                                                  
  ssh_interface = "public_ip"                                                                        
  ssh_pty       = "true"                                                                             
  ssh_timeout   = "20m"                                                                              
  ssh_username  = "${var.ssh_username}"                                                              
  security_group_id = "${var.security_group_id}"                                                     
  subnet_id     = "${var.subnet_id}"                                                                 
  tags = {                                                                                           
    BuiltBy = "bharat-ami-poc"                                                                        
    Name    = "${var.ami_name}"                                                                      
  }                                                                                                  
  vpc_id = "${var.vpc_id}"                                                                           
}                                                                                                    
                                                                                                     
build {                                                                                              
  description = "AWS image"                                                                          
                                                                                                     
  sources = ["source.amazon-ebs.autogenerated_1"]                                                    
  provisioner "ansible" {                                                                            
    playbook_file = "yugabyte-db/managed/devops/yb_custom_ami_builder.yml"    
    use_proxy = false                                                                                
    ansible_env_vars = [ "ANSIBLE_CONFIG=yugabyte-db/managed/devops/ansible.cfg", "yb_devops_home=yugabyte-db/managed/devops" ]
    extra_arguments = ["--tags", "yb-prebuilt-ami", "--extra-vars", "@yugabyte-db/managed/devops/vars.yml"]
  }                                                                                                  
}